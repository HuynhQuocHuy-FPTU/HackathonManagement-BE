package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;

import com.hackathon.email.MailRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor

public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final AccountRepository accRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final ExpertRepository expertRepository;

    private final EmailService emailService;
    private final AuditService auditService;
    private static final int MAX_TEAM_SIZE = 5;
    private static final long LOCK_BEFORE_DEADLINE_HOURS = 24;
    private static final long INVITATION_EXPIRE_HOURS = 3;


    // Nếu Đội đã nộp đơn và thời gian hiện tại cách thời gian đk event dưới 24 giờ -> CHẶN
    // Check đơn đăng ký và thời hạn 24h của Team dựa trên Event đã gửi đơn.
    // Nếu như chưa dk thì có quyền thay đổi tùy thích
    public void checkEventRegistrationWindow(Team team) {
        List<Registration> registrations = registrationRepository.findByTeam(team);
        if (registrations != null && !registrations.isEmpty()) {
            boolean isPastDeadline = registrations.stream()
                    .anyMatch(regis -> {
                        // Chỉ cần đơn đó hợp lệ (PENDING/APPROVED)
                        if (regis.getStatus() == RegistrationStatus.PENDING || regis.getStatus() == RegistrationStatus.APPROVED) {
                            HackathonEvent event = regis.getHackathonEvent();
                            if (event != null && event.getRegistrationDeadline() != null) {
                                // CHECK: Nếu thời gian hiện tại đã vượt qua (Deadline - LOCK_HOURS)
                                return LocalDateTime.now().isAfter(event.getRegistrationDeadline().minusHours(LOCK_BEFORE_DEADLINE_HOURS));
                            }
                        }
                        return false;
                    });

            if (isPastDeadline) {
                throw new BadRequestException("Hệ thống đã đóng cổng thay đổi thông tin do cuộc thi đã bước vào giai đoạn chốt sổ (Trước deadline " + LOCK_BEFORE_DEADLINE_HOURS + " giờ).");
            }

        }

    }

    /*
     *TẠO TEAM, LỜI MỜI
     */

    //FUNCTION 1:Create Team
    //BR: Khi tao team phai co tieu thieu it nhat 1 thanh vien duoc moi (bao gom leader va 1 thanh vien khac)

    @Transactional
    @Override
    public TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetail) {
        // 1. Lay thong tin cua account dang login (Nguoi tao tem se duoc gan role la leader)
        Account leaderAccount = userDetail.getAccount();
        // 2. Check account đang tạo team có tham gia team khác không
        // 2.1 Nếu account này thuộc Team khác mà có trạng thái DRAFT || BUSY thì ko được phép tạo team mới
        // 2.2 Chỉ được tạo Team mới, tham gia team mới khi team cũ có trạng thai FINISH
        List<TeamMember> existingTeams = teamMemberRepository.findByStudent(leaderAccount.getStudent());
        if (!existingTeams.isEmpty()) {
            for (TeamMember teamMember : existingTeams) {
                TeamStatus teamStatus = teamMember.getTeam().getStatus();
                if (teamMember.getIsLeader()) {
                    throw new BadRequestException(
                            "Bạn đang là Leader, không thể tạo Team mới. Vui lòng chuyển quyền hoặc giải tán team trước khi tạo team mới.");
                }

                if (teamStatus == TeamStatus.DRAFT) {
                    throw new BadRequestException("Bạn không thể tạo Team mới do bạn đang tham gia một team khác. " +
                            "Vui lòng rời Team cũ trước khi tạo team mới.");
                }

                if (teamStatus == TeamStatus.BUSY) {
                    throw new BadRequestException("Bạn không thể tạo Team mới do bạn đang tham gia một cuộc thi chưa kết thúc. " +
                            "Vui lòng đợi khi cuộc thi kết thúc để tiến hành việc tạo Team.");
                }
            }
        }


        // 3. BR: bắt buộc mời ít nhất 1 người khác
        if (request.getMemberEmails() == null || request.getMemberEmails().isEmpty()) {
            throw new BadRequestException("Khi tạo đội, bạn bắt buộc phải mời ít nhất 1 thành viên khác tham gia!");
        }

        // 3.2  Check duplicate member and loc email
        Set<String> cleanEmails = new HashSet<>();
        for (String email : request.getMemberEmails()) {
            if (email != null && !email.isBlank()) {
                cleanEmails.add(email.trim());
            }
        }

        if (cleanEmails.isEmpty()) {
            throw new BadRequestException("Danh sách email mời vào nhóm không hợp lệ!");
        }

        //3.3 Check Email nhập vào có hợp lệ không (nếu là Leader của nhóm mình thì ko được )
        if (cleanEmails.contains(leaderAccount.getEmail().trim())) {
            throw new BadRequestException("Bạn là Trưởng nhóm, không cần tự mời chính mình!");
        }

        //3.4   Check account được gửi mail nếu ko có role Là STUDENT thì ko được phép nhập
        Map<String, Account> memberAccountMap = new HashMap<>();
        for (String memberEmail : cleanEmails) {
            Account checkAcc = accRepository.findByEmail(memberEmail)
                    .orElseThrow(() -> new BadRequestException("Tài khoản với email " + memberEmail + " không tồn tại trên hệ thống."));
            if (checkAcc.getRole() != AccountRole.STUDENT) {
                throw new BadRequestException("Email " + memberEmail + " không hợp lệ. Bạn chỉ có thể mời tài khoản có vai trò là STUDENT.");
            }
            memberAccountMap.put(memberEmail, checkAcc);
        }


        //3.5 Check trùng tên Nhóm
        boolean existName = teamRepository.existsByTeamNameIgnoreCase(request.getTeamName().trim());
        if (existName) {
            throw new BadRequestException("Tên nhóm này đã được đăng ký trong cuộc thi này rồi!");
        }

        //4. Create Team
        Team team = new Team();
        team.setTeamName(request.getTeamName().trim());
        team.setStatus(TeamStatus.DRAFT);
        team.setTeamSize(1);
        Team saveTeam = teamRepository.save(team);


        //4.1. Luu thong tin Leader
        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(saveTeam);
        leaderMember.setIsLeader(true);
        leaderMember.setStudent(leaderAccount.getStudent());
        teamMemberRepository.save(leaderMember);

        List<TeamResponse.MemberInfo> listMember = new ArrayList<>();
        List<String> invitedEmails = new ArrayList<>();
        //5. Tạo object, save info of leader vao ListMember
        TeamResponse.MemberInfo leaderInfo = TeamResponse.MemberInfo.builder()
                .studentCode(leaderAccount.getStudent().getStudentCode())
                .fullName(leaderAccount.getStudent().getStudentName())
                .email(leaderAccount.getEmail())
                .major(leaderAccount.getStudent().getMajor()).build();
        listMember.add(leaderInfo);

        //6. Tao loi moi gui toi cac thah vien
        for (String memberEmail : cleanEmails) {
            Account account = memberAccountMap.get(memberEmail);
            Notification invite = new Notification();
            invite.setAccount(account);
            invite.setTeam(saveTeam);
            invite.setType(NotificationType.TEAM_INVITATION);
            invite.setStatus(InvitationStatus.PENDING);
            invite.setTitle("INVITE TEAM " + saveTeam.getTeamName().trim());
            invite.setMessage("Bạn được mời bởi " + leaderAccount.getStudent().getStudentName() +
                    " để tạo đội  tham gia cuộc thi Hackathon.");
            Notification savedNoti = notificationRepository.save(invite);
            invitedEmails.add(memberEmail);

            //7. Gui loi moi den cac thnah vien
            try {
                MailRequest mailRequest = new MailRequest();
                mailRequest.setTo(account.getEmail());
                mailRequest.setSubject("FPT HACKATHON - Team Invitation: " + saveTeam.getTeamName());

                Map<String, Object> props = new HashMap<>();
                props.put("studentName", account.getStudent().getStudentName());
                props.put("teamName", saveTeam.getTeamName().trim());
                props.put("leaderName", leaderAccount.getStudent().getStudentName());
                props.put("email", leaderAccount.getEmail());
                props.put("receiverEmail", account.getEmail());
                props.put("notificationId", savedNoti.getId());
                mailRequest.setProps(props);
                emailService.sendEmail(mailRequest, "invitation");

            } catch (Exception e) {
                System.out.println("Email error: " + e.getMessage());
            }

        }
        auditService.saveLog(
                leaderAccount,
                AuditAction.CREATE_TEAM,
                AuditEntityType.TEAM,
                team.getTeamId(),
                "Create team " + team.getTeamName()
        );
        //8. Return TeamResponse
        return new TeamResponse(saveTeam.getTeamId(), saveTeam.getTeamName(), leaderInfo, listMember, saveTeam.getCreateAt(), invitedEmails);
    }

    @Transactional
    @Override
    public TeamResponse sendTeamInvitation(CreateTeamRequest request, CustomUserDetails userDetails) {
        // 1. Leader gửi lời mời đến thành viên mình mong muốn
        Account leaderAcc = userDetails.getAccount();
        if (leaderAcc == null || leaderAcc.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản leader không hợp lệ.");
        }

        TeamMember teamMember = teamMemberRepository.findByTeam_TeamIdAndStudent(request.getTeamId(), leaderAcc.getStudent())
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền mời thành viên (Bạn không phải Leader hoặc không thuộc đội này"));

        if (!teamMember.getIsLeader()) {
            throw new BadRequestException("Bạn không phải leader, bạn không được phép mời thành viên khác.");
        }

        Team team = teamMember.getTeam();

        //2.
        if (team.getStatus() == TeamStatus.FINISHED) {
            throw new BadRequestException("Đội hình này đã kết thúc vòng đời thi đấu, không thể mời thêm!");
        }
        this.checkEventRegistrationWindow(team);

        // 3.  Check duplicate member and loc email
        Set<String> cleanEmails = new HashSet<>();
        for (String email : request.getMemberEmails()) {
            if (email != null && !email.isBlank()) {
                cleanEmails.add(email.trim());
            }
        }
        if (cleanEmails.isEmpty()) {
            throw new BadRequestException("Danh sách email mời vào nhóm không hợp lệ!");
        }
        //3.1 Check Email nhập vào có hợp lệ không (nếu là Leader của nhóm mình thì ko được )
        if (cleanEmails.contains(leaderAcc.getEmail().trim())) {
            throw new BadRequestException("Bạn là Trưởng nhóm, không cần tự mời chính mình!");
        }

        //6. Tao loi moi gui toi cac thah vien
        List<String> successfulInvites = new ArrayList<>();
        // Lấy danh sách EventIds mà Team này đã đăng ký để check trùng
        List<Registration> teamRegistrations = registrationRepository.findByTeam(team);
        List<Integer> eventIds = teamRegistrations.stream()
                .map(r -> r.getHackathonEvent().getEventId()).toList();

        for (String memberEmail : cleanEmails) {
            Account memberAccount = accRepository.findByEmail(memberEmail.trim())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản của email: " + leaderAcc.getEmail()));
            List<TeamMember> studentTeams = teamMemberRepository.findByStudent(memberAccount.getStudent());

            // Chặn gửi mail đến những ng ko có role là Studnet
            if (memberAccount.getRole() != AccountRole.STUDENT) {
                throw new BadRequestException("Bạn không được phép gửi mail đến những tài khoản không phải là STUDENT.");
            }

            for (TeamMember tm : studentTeams) {
                if (tm.getTeam().getStatus() != TeamStatus.FINISHED) {
                    // Check trùng event
                    List<Registration> regs = registrationRepository.findByTeam(tm.getTeam());
                    if (regs.stream().anyMatch(r -> eventIds.contains(r.getHackathonEvent().getEventId()))) {
                        throw new BadRequestException("Sinh viên  đã tham gia một đội thi khác trong cùng sự kiện.");
                    }
                    if (tm.getTeam().getStatus() == TeamStatus.BUSY) {
                        throw new BadRequestException("Sinh viên hiện đang bận ở đội khác.");
                    }
                }

            }

            Notification invite = new Notification();
            invite.setAccount(memberAccount);
            invite.setTeam(team);
            invite.setType(NotificationType.TEAM_INVITATION);
            invite.setStatus(InvitationStatus.PENDING);
            invite.setTitle("INVITE TEAM " + team.getTeamName());
            invite.setMessage("Bạn được mời bởi " + leaderAcc.getStudent().getStudentName() +
                    " để tạo đội  tham gia cuộc thi Hackathon.");
            Notification savedNoti = notificationRepository.save(invite);
            successfulInvites.add(memberEmail);

            //7. Gui loi moi den cac thnah vien
            try {
                MailRequest mailRequest = new MailRequest();
                mailRequest.setTo(memberAccount.getEmail());
                mailRequest.setSubject("FPT HACKATHON - Team Invitation: " + team.getTeamName());

                Map<String, Object> props = new HashMap<>();
                props.put("studentName", memberAccount.getStudent().getStudentName());
                props.put("teamName", team.getTeamName());
                props.put("leaderName", leaderAcc.getStudent().getStudentName());
                props.put("email", leaderAcc.getEmail());
                props.put("receiverEmail", memberAccount.getEmail());
                props.put("notificationId", savedNoti.getId());
                mailRequest.setProps(props);
                emailService.sendEmail(mailRequest, "invitation");

            } catch (Exception e) {
                System.out.println("Email error: " + e.getMessage());
            }

        }
        //8. Return TeamResponse
        return TeamResponse.builder()
                .teamId(team.getTeamId())
                .teamName(team.getTeamName())
                .createAt(team.getCreateAt())
                .invitedEmails(successfulInvites).build();


    }


    //FUNCTION 2:UPDATE INFORMATION ABOUT TEAM AS NAME
    @Override
    @Transactional
    public String updateInfo(CustomUserDetails userDetails, String teamName) {

        // 1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        Account currentAccount = userDetails.getAccount();

        //2. Check account nay co tham gia Team này không
        List<TeamMember> currentMember = teamMemberRepository.findByStudent(currentAccount.getStudent());
        if (currentMember.isEmpty()) {
            throw new BadRequestException("Bạn hiện chưa tham gia bất kỳ đội nào trong hệ thống!");
        }
        TeamMember leaderRole = currentMember.stream()
                .filter(tm -> tm.getTeam().getStatus() != TeamStatus.FINISHED) // Chỉ xét nhóm đang DRAFT hoặc BUSY
                .filter(TeamMember::getIsLeader)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Bạn chỉ là thành viên, không phải là trưởng nhóm. Bạn không có quyền thay đổi thông tin đội!"));

        // Lấy ra đối tượng Team từ dòng Leader tìm được
        Team team = leaderRole.getTeam();
        if (team.getStatus() != TeamStatus.DRAFT) {
            throw new BadRequestException(
                    "Chỉ được chỉnh sửa Team khi ở trạng thái DRAFT."
            );
        }
        // 4. Check Team này đã được phê duyệt đội khi gửi đơn đăng ký chưa
        List<Registration> registration = registrationRepository.findByTeam(team);
        if (registration != null && !registration.isEmpty()) {
            boolean hasActiveRegister = registration.stream().anyMatch(regis ->
                    regis.getStatus().equals(RegistrationStatus.APPROVED) || regis.getStatus().equals(RegistrationStatus.PENDING));
            if (hasActiveRegister) {
                throw new BadRequestException("Bạn không được phép thay đổi tên nhóm khi đã gửi đơn đăng ký Team.");
            }
        }

        // 5.Check deadline
        checkEventRegistrationWindow(team);

        // 5.1 Validate team name
        if (teamName == null
                || teamName.isBlank()) {
            throw new BadRequestException(
                    "Tên đội mới không được để trống!");
        }
        String cleanName = teamName.trim();
        if (cleanName.equalsIgnoreCase(team.getTeamName().trim())) {
            return team.getTeamName();
        }
        boolean existName = teamRepository.existsByTeamNameIgnoreCaseAndTeamIdNot(cleanName, team.getTeamId());
        if (existName) {
            throw new BadRequestException("Tên nhóm '" + cleanName + "' đã được đăng ký bởi một đội khác trong hệ thống rồi!");
        }

        // 6. Update
        team.setTeamName(cleanName);
        teamRepository.save(team);
        auditService.saveLog(
                currentAccount,
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                team.getTeamId(),
                "Update team " + team.getTeamName()
        );
        return team.getTeamName();

    }

    //FUNCTION 3: RỜI TEAM

    @Transactional
    @Override
    public void leaveTeam(CustomUserDetails userDetails, Integer teamId) {
        //1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        Account currentUser = userDetails.getAccount();
        Student student = currentUser.getStudent();
        //1.1 Check Student có đang thuộc Team nào không
        TeamMember teamMember = teamMemberRepository.findByTeam_TeamIdAndStudent(teamId, student)
                .orElseThrow(() -> new BadRequestException("Bạn hiện không tham gia hoặc không phải thành viên của đội này!"));

        //2. Leader khong duoc phep roi khoi nhom , truoc khi chuyen quyen leader cho nguoi khac
        if (teamMember.getIsLeader()) {
            throw new BadRequestException("Leader không được phép rời Team trước khi chuyển quyền cho thành viên khác.");
        }

        Team team = teamMember.getTeam();

        //3. Check deadline
        checkEventRegistrationWindow(team);
        // Xoa
        teamMemberRepository.delete(teamMember);
        auditService.saveLog(
                currentUser,
                AuditAction.UPDATE_TEAM,
                AuditEntityType.TEAM,
                team.getTeamId(),
                "Leave team " + team.getTeamName()
        );

        //5. Cập nhật lại số lượng thành viên thực tế trong DB
        team.setTeamSize(Math.max(0, team.getTeamSize() - 1));
        teamRepository.save(team);

    }


    //FUNCTION 4: CHUYỂN QUYỀN LEADER(Chỉ mới gửi lời mời đến thành viên muốn chuyển quyền )
    @Override
    public void transferLeader(Integer teamId, TeamRequest request, CustomUserDetails userDetails) {

        // 2. Check Team
        Account currentUser = userDetails.getAccount();
        TeamMember teamMember = teamMemberRepository.findByTeam_TeamIdAndStudent(teamId, currentUser.getStudent())
                .orElseThrow(() -> new BadRequestException("Bạn hiện không tham gia hoặc không phải thành viên của đội này!"));
        Team team = teamMember.getTeam();

        //3. Check Team đã được phê duyệt chưa(xem lại bussiness rule)
        //4. Check Leader có thuộc Team ko
        if (!teamMember.getIsLeader()) {
            throw new BadRequestException("Chỉ Leader hiện tại mới được quyền chuyển quyền Trưởng nhóm.");
        }

        // 5.CheckDeadline
        this.checkEventRegistrationWindow(team);

        //5. Check Student được chuyển quyền có thuộc Team ko
        Student newLeader = studentRepository.findByStudentCode(request.getStudentCode());
        if (newLeader == null) {
            throw new BadRequestException("Không tìm thấy sinh viên được chọn.");
        }
        //6. Check Student được chuyển quyền có thuộc  team này không
        boolean isStudent = teamMemberRepository.existsByTeamAndStudent(team, newLeader);
        if (!isStudent) {
            throw new BadRequestException("Sinh viên không thuộc Team này.");
        }

        //6. Check leader không tự chuyển quyền cho mình
        if (currentUser.getStudent().getStudentCode().equalsIgnoreCase(newLeader.getStudentCode())) {
            throw new BadRequestException("Bạn đang là Leader của team. Bạn không thể tự chuyển quyền cho chính mình.");
        }

        // 8. Tạo thông báo gửi lời mời
        Notification inviteTransfer = new Notification();

        inviteTransfer.setAccount(newLeader.getAccount());
        inviteTransfer.setTeam(team);
        inviteTransfer.setType(NotificationType.LEADER_TRANSFER_REQUEST);
        inviteTransfer.setTitle("TRANSFER LEADER.");
        inviteTransfer.setMessage("Bạn được mời làm trưởng nhóm " + team.getTeamName());
        inviteTransfer.setStatus(InvitationStatus.PENDING);
        Notification savedNoti = notificationRepository.save(inviteTransfer);

        System.out.println(
                "Notification ID = " + savedNoti.getId());
        // 9. Gửi lời mời
        try {
            MailRequest mailRequest = new MailRequest();
            mailRequest.setTo(newLeader.getAccount().getEmail());
            mailRequest.setSubject("FPT HACKATHON. Lời mời chuyển quyền Leader cho cuộc thi Hackathon" +
                    " đến từ Team " + team.getTeamName());
            Map<String, Object> props = new HashMap<>();
            props.put("studentName", newLeader.getStudentName());
            props.put("teamName", team.getTeamName());
            props.put("leaderName", currentUser.getStudent().getStudentName());
            props.put("email", currentUser.getEmail());
            props.put("receiverEmail", newLeader.getAccount().getEmail());
            props.put("notiId", savedNoti.getId());
            mailRequest.setProps(props);
            emailService.sendEmail(mailRequest, "transfer");
        } catch (Exception e) {
            System.out.println("==> Lỗi gửi email chuyển quyền leader: " + e.getMessage());
        }
        auditService.saveLog(
                currentUser,
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                team.getTeamId(),
                "Transfer leader " + team.getTeamName() + "leader mới: " + newLeader
        );
    }

    //FUNCTION 5: HÀM XỬ LÝ CHẤP NHẬN LỜI MỜI CHO TRANSFER, INVITE TEAM
    @Override
    public void acceptGeneralInvite(Long notificationId, CustomUserDetails userDetails) {
        //1.Tìm lời mời dựa trên thông báo
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại hoặc đã bị hủy từ trước."));
        ;
        // 2. Check account được nhận lời mời vs account được gửi lời mời có giống nhau không(nhớ mở ra)
        if (userDetails != null && userDetails.getAccount() != null) {
            if (notification.getAccount().getAccountId() != userDetails.getAccount().getAccountId()) {
                throw new BadRequestException("Bạn không có quyền truy cập vào thông báo này.");
            }
        }

        // 3. Đánh dấu thông báo đã đọc
        if (!notification.isRead()) {
            notification.setRead(true);
        }

        //4. Check trạng thái của lời mời(hết hạn, chấp nhận, từ chối) rồi sẽ vô hiệu hóa
        if (notification.getStatus() == InvitationStatus.ACCEPTED
                || notification.getStatus() == InvitationStatus.REJECTED
                || notification.getStatus() == InvitationStatus.EXPIRED
                || notification.getStatus() == InvitationStatus.INVALID) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc không còn hiệu lực.");
        }
        // 4.1 Check trường hợp Team ko tồn tại
        Team team = notification.getTeam();
        if (team == null || team.getTeamSize() == null || team.getTeamSize() <= 0) {
            notification.setStatus(InvitationStatus.INVALID);
            notificationRepository.save(notification);
            throw new BadRequestException("Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
        }

        // Check xem đội này đã gửi đơn đăng ký cuôc thi  nào chưa
        this.checkEventRegistrationWindow(team);
        //6.
        switch (notification.getType()) {
            case TEAM_INVITATION:
                this.acceptInvite(notification, userDetails);
                break;

            case LEADER_TRANSFER_REQUEST:
                this.acceptLeaderTransfer(notification, userDetails);
                break;

            default:
                throw new BadRequestException("Loại thông báo không hợp lệ.");
        }
        notificationRepository.save(notification);

    }


    @Override
    @Transactional(dontRollbackOn = BadRequestException.class) // Khong roll Back khi dinh loi xu ly tb hong
    public void acceptInvite(Notification notification, CustomUserDetails userDetails) {
        //1. Tim thong bao or loi moi tuong ung
        //2. Kiem tra team gui loi moi con ton tai khong
        Team team = teamRepository.findById(notification.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại"));

        //3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Account inviteAccount = notification.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }
        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        LocalDateTime expiredAt = notification.getCreatedAt().plusDays(3);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            notification.setTitle("EXPIRED. Lời mời tham gia : " + team.getTeamName() + " hết hạn.");
            notification.setMessage("Lời mời này có thời hạn trong vòng " + INVITATION_EXPIRE_HOURS + " ngày");
            notification.setStatus(InvitationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }
        //5.Kiểm tra xem sinh viên này có bị TRÙNG cuộc thi (Event) không
        List<TeamMember> userCurrentTeams = teamMemberRepository.findByStudent(inviteAccount.getStudent());
        if (userCurrentTeams != null && !userCurrentTeams.isEmpty()) {
            // Lấy trước danh sách các đơn đăng ký (giải đấu) của Team mới chuẩn bị gia nhập
            List<Registration> newTeamRegistrations = registrationRepository.findByTeam(team);
            List<Integer> newTeamEventIds = (newTeamRegistrations != null) ? newTeamRegistrations.stream()
                                                                             .map(r -> r.getHackathonEvent().getEventId()).toList() : List.of();

            for (TeamMember tm : userCurrentTeams) {
                // Chỉ xét các đội ĐANG HOẠT ĐỘNG (Bỏ qua các đội FINISHED trong quá khứ)
                if (tm.getTeam().getStatus() != TeamStatus.FINISHED) {

                    //  4.1: Check trùng giải đấu nếu cả 2 đội đều đã đăng ký đi thi giải đó
                    List<Registration> currentTeamRegis = registrationRepository.findByTeam(tm.getTeam());
                    if (currentTeamRegis != null && !newTeamEventIds.isEmpty()) {
                        boolean isDuplicatedEvent = currentTeamRegis.stream()
                                .anyMatch(r -> newTeamEventIds.contains(r.getHackathonEvent().getEventId()));
                        if (isDuplicatedEvent) {
                            throw new BadRequestException("Bạn đã tham gia một đội thi khác trong cùng cuộc thi này rồi, không thể gia nhập thêm!");
                        }
                    }

                    //  4.2: Nếu sinh viên đang bận thi đấu  ở giải khác thì cũng chặn luôn
                    if (tm.getTeam().getStatus() == TeamStatus.BUSY) {
                        throw new BadRequestException("Bạn đang trong trạng thái thi đấu ở một đội hình khác, không thể gia nhập đội này!");
                    }
                }
            }
        }


        // 6. Check so luong thanh vien hien tai cua nhom
        int currentSize = Optional.ofNullable(team.getTeamSize()).orElse(0);
        if (currentSize >= MAX_TEAM_SIZE) {
            notification.setTitle("INVALID. Team đã đủ thành viên");
            notification.setMessage("Lời mời này không còn hiệu lực vì Đội thi đã đủ thành viên.");
            notification.setStatus(InvitationStatus.INVALID);
            notificationRepository.save(notification);
            throw new BadRequestException("Team '" + team.getTeamName() + " đủ thành viên");
        }

        // 7. Check ng dung da la thanh vien cua nhom hay chua
        boolean exists = teamMemberRepository.existsByTeamAndStudent(team, inviteAccount.getStudent());
        if (exists) {
            throw new BadRequestException("Bạn đã là thành viên của Team này từ trước rồi.");
        }

        // 8. Them vao team
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setIsLeader(false);
        member.setStudent(inviteAccount.getStudent());
        teamMemberRepository.save(member);

        // 9. Cap nhat so luong thanh vien
        team.setTeamSize(Optional.ofNullable(team.getTeamSize()).orElse(0) + 1);
        teamRepository.save(team);

        // 10. Cap nhat thong boa khi ban Chap nhan loi moi
        notification.setTitle("INVITATION ACCEPTED. Bạn đã tham gia Team: " + team.getTeamName());
        notification.setMessage("Thành viên chính thức của " + team.getTeamName());
        notification.setStatus(InvitationStatus.ACCEPTED);
        notificationRepository.save(notification);

        // 11. Check All Team, neu du 5 thanh vien , vo hieu hoa loi moi con lai
        if (team.getTeamSize() == MAX_TEAM_SIZE) {
            List<Notification> otherInvites = notificationRepository.findByTeam(team);
            for (Notification oldNoti : otherInvites) {
                if (oldNoti.getId().equals(notification.getId())) {
                    continue;
                }
                // Vo hieu hoa loi moi con lai
                if (oldNoti.getType() == NotificationType.TEAM_INVITATION && oldNoti.getStatus() == InvitationStatus.PENDING) {
                    oldNoti.setStatus(InvitationStatus.INVALID);
                    oldNoti.setTitle("INVALID. Lời mời vào đội " + team.getTeamName());
                    oldNoti.setMessage("This invitation is no longer valid because the team has reached its maximum capacity.");
                    oldNoti.setRead(true);
                    notificationRepository.save(oldNoti);
                }

            }
        }

    }


    @Override
    @Transactional(dontRollbackOn = BadRequestException.class) // Khong roll Back khi dinh loi xu ly tb hong
    public void acceptLeaderTransfer(Notification notification, CustomUserDetails userDetails) {
        //1. Tim tb hoac loi moi tuong ung
        //2. Kiem tra Team loi moi con ton tai khong
        Team team = teamRepository.findById(notification.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        //3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Account inviteAccount = notification.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }
        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        LocalDateTime expiredAt = notification.getCreatedAt().plusDays(3);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            notification.setTitle("EXPIRED. Lời mời tham gia : " + team.getTeamName() + " hết hạn.");
            notification.setMessage("Lời mời này có thời hạn trong vòng " + INVITATION_EXPIRE_HOURS + " ngày");
            notification.setStatus(InvitationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }
        Student newLeaderStudent = inviteAccount.getStudent();

        //5. Check người leader gửi lời mời vs ng làm leader hiện tại có trùng khớp ko
        // Tránh trường hợp gửi lời mời cho 2 người và ng kia đồng ý trước
        TeamMember currentLeader = teamMemberRepository.findByTeamAndIsLeader(team, true)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy leader hiện tại"));

        if (currentLeader.getStudent().getStudentCode().equalsIgnoreCase(newLeaderStudent.getStudentCode())) {
            throw new BadRequestException("Bạn đã là Trưởng nhóm của đội này rồi.");
        }

        // 6. Update leader mới
        currentLeader.setIsLeader(false);

        TeamMember newLeader = teamMemberRepository.findByTeamAndStudent(team, newLeaderStudent)
                .orElseThrow(() -> new BadRequestException("Bạn hiện không phải là thành viên của đội này nên không thể nhận quyền Trưởng nhóm."));

        newLeader.setIsLeader(true);
        teamMemberRepository.save(currentLeader);
        teamMemberRepository.save(newLeader);
        notification.setTitle("TRANSFER APPROVED. Bạn đã là Leader của Team: " + team.getTeamName());
        notification.setMessage("Bạn đã chấp nhận lời mời và chính thức trở thành Trưởng nhóm.");
        notification.setStatus(InvitationStatus.ACCEPTED);

    }

    @Override
    @Transactional
    public void rejectGeneralInvite(Long notificationId, CustomUserDetails userDetails) {
        System.out.println("STEP 1");


        //1.Tìm lời mời dựa trên thông báo
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại hoặc đã bị hủy từ trước."));
        ;
        System.out.println("STEP 2");

        // 2. Check account được nhận lời mời vs account được gửi lời mời có giống nhau không
        if (userDetails != null && userDetails.getAccount() != null) {
            if (notification.getAccount().getAccountId() != userDetails.getAccount().getAccountId()) {
                throw new BadRequestException("Bạn không có quyền truy cập vào thông báo này.");
            }
        }

        // 3. Đánh dấu thông báo đã đọc
        if (!notification.isRead()) {
            notification.setRead(true);
        }

        //4. Check trạng thái của lời mời(hết hạn, chấp nhận, từ chối) rồi sẽ vô hiệu hóa
        if (notification.getStatus() == InvitationStatus.ACCEPTED
                || notification.getStatus() == InvitationStatus.REJECTED
                || notification.getStatus() == InvitationStatus.EXPIRED
                || notification.getStatus() == InvitationStatus.INVALID) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc không còn hiệu lực.");
        }
        // 4.1 Check trường hợp Team ko tồn tại
        Team team = notification.getTeam();
        if (team == null || team.getTeamSize() == null || team.getTeamSize() <= 0) {
            notification.setStatus(InvitationStatus.INVALID);
            notificationRepository.save(notification);
            throw new BadRequestException("Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
        }

//        5. Check thời hạn(ĐK: CÙNG BUSINESS RULE)
        checkEventRegistrationWindow(team);

        switch (notification.getType()) {
            case TEAM_INVITATION:
                this.rejectTeamInvite(notification, userDetails);
                break;

            case LEADER_TRANSFER_REQUEST:
                this.rejectLeaderTransferInvite(notification, userDetails);
                break;
            default:
                throw new BadRequestException("Loại thông báo không hợp lệ để thực hiện thao tác từ chối.");
        }
        notificationRepository.save(notification);
    }

    @Override
    public void rejectLeaderTransferInvite(Notification notification, CustomUserDetails userDetails) {
        //1. Tim tb hoac loi moi tuong ung
        //2. Kiem tra Team loi moi con ton tai khong
        Team team = teamRepository.findById(notification.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        //3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Account inviteAccount = notification.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }
        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        LocalDateTime expiredAt = notification.getCreatedAt().plusDays(3);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            notification.setTitle("EXPIRED. Lời mời tham gia : " + team.getTeamName() + " hết hạn.");
            notification.setMessage("Lời mời này có thời hạn trong vòng " + INVITATION_EXPIRE_HOURS + " ngày");
            notification.setStatus(InvitationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }
        Student newLeaderStudent = inviteAccount.getStudent();
        notification.setTitle("TRANSFER REJECTED. Tôi từ chối làm Leader Team: " + team.getTeamName());
        notification.setMessage("Bạn đã từ chối lời mời chuyển quyền Leader.");
        notification.setStatus(InvitationStatus.REJECTED);


    }

    @Override
    public void rejectTeamInvite(Notification notification, CustomUserDetails userDetails) {
        //1. Tim tb hoac loi moi tuong ung
        //2. Kiem tra Team loi moi con ton tai khong
        Team team = teamRepository.findById(notification.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        //3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Account inviteAccount = notification.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }

        System.out.println("Current Account: "
                + userDetails.getAccount().getAccountId());

        System.out.println("Notification Account: "
                + notification.getAccount().getAccountId());
        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        LocalDateTime expiredAt = notification.getCreatedAt().plusDays(3);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            notification.setTitle("EXPIRED. Lời mời tham gia : " + team.getTeamName() + " hết hạn.");
            notification.setMessage("Lời mời này có thời hạn trong vòng " + INVITATION_EXPIRE_HOURS + " ngày");
            notification.setStatus(InvitationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }
        Student newLeaderStudent = inviteAccount.getStudent();
        notification.setTitle("INVITE REJECTED. Tôi từ chối lời mời tham gia nhóm : " + team.getTeamName());
        notification.setMessage("Bạn đã từ chối lời mời tham gia nhóm.");
        notification.setStatus(InvitationStatus.REJECTED);

    }

      /*
    XEM THÔNG TIN VỀ TEAM
     */

    @Override
    public TeamDetailResponse getTeamMember(Integer teamId, CustomUserDetails userDetails) {
        //1. Check team có tồn tại không
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        //2. Check account đang đăng nhập có đag  là thành viên của Team đó hay không
        Account currentAccount = userDetails.getAccount();
        TeamMember teamMember = teamMemberRepository.findByTeamAndStudent(team, currentAccount.getStudent())
                .orElseThrow(() -> new BadRequestException("Sinh viên hiện tại không thuộc Team này. Không được phép xem danh sách Team này."));
        //3. Lấy danh sách teamMember
        List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
        TeamDetailResponse.MemberInfo leaderInfo = null;
        List<TeamDetailResponse.MemberInfo> officialMembers = new ArrayList<>();

        for (TeamMember member : teamMembers) {
            TeamDetailResponse.MemberInfo info = TeamDetailResponse.MemberInfo.builder()
                    .studentCode(member.getStudent().getStudentCode())
                    .fullName(member.getStudent().getStudentName())
                    .email(member.getStudent().getAccount().getEmail())
                    .major(member.getStudent().getMajor())
                    .build();

            if (member.getIsLeader()) {
                leaderInfo = info;
            } else {
                officialMembers.add(info); // Chỉ add thành viên thường vào list này
            }
        }


        // 4. Lấy danh sách các email đã gửi lời mời
        List<TeamDetailResponse.InviteInfo> inviteInfo = new ArrayList<>();
        // Chỉ khi người đang xem là LEADER  thì mới xem được lời mời
        if (teamMember.getIsLeader()) {
            List<Notification> invites = notificationRepository.findByTeamAndType(team, NotificationType.TEAM_INVITATION);
            for (Notification invite : invites) {
                inviteInfo.add(new TeamDetailResponse.InviteInfo(
                        invite.getAccount().getEmail(),
                        invite.getStatus().name()
                ));
            }
        }

        // 4. Đóng gói dữ liệu trả về cho Frontend
        return TeamDetailResponse.builder()
                .teamId(team.getTeamId())
                .teamName(team.getTeamName())
                .leader(leaderInfo)
                .members(officialMembers).createAt(team.getCreateAt()).invitations(inviteInfo)
                .build();
    }

    @Override
    public List<TeamDetailResponse> getTeamForAdmin(CustomUserDetails userDetails) {
        //1. Check admin
        Account account = userDetails.getAccount();
        if (account.getRole() != AccountRole.ADMIN) {
            throw new BadRequestException("Bạn không phải là Admin, bạn không được phép xem danh sách này.");
        }

        //2. Lấy list team
        List<Team> listTeam = teamRepository.findAll();
        List<TeamDetailResponse.MemberInfo> members = new ArrayList<>();
        TeamDetailResponse.MemberInfo leader = null;
        return listTeam.stream().map(team -> {
            String leaderName = team.getTeamMembers().stream()
                    .filter(TeamMember::getIsLeader) // Lọc người có isLeader == true
                    .map(tm -> tm.getStudent().getStudentName())
                    .findFirst()
                    .orElse("Chưa có Leader");
            TeamDetailResponse.MemberInfo leaderInfo = TeamDetailResponse.MemberInfo.builder()
                    .fullName(leaderName)
                    .build();
            int size = team.getTeamMembers() != null ? team.getTeamMembers().size() : 0;
            String statusStr = team.getStatus().name();
            return TeamDetailResponse.builder()
                    .teamId(team.getTeamId())
                    .teamName(team.getTeamName())
                    .leader(leaderInfo)
                    .createAt(team.getCreateAt())
                    .sizeTeam(size)
                    .status(statusStr)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public TeamDetailResponse getTeamDetail(Integer teamId, CustomUserDetails userDetails) {
        //1. Check admin
        Account account = userDetails.getAccount();
        if (account.getRole() != AccountRole.ADMIN
                && account.getRole() != AccountRole.EXPERT
                && account.getRole() != AccountRole.EVENTCOORDINATOR) {
            throw new BadRequestException("Bạn không có quyền  xem danh sách này. Chỉ có ADMIN, EVENT COORDINATOR , EXPERT mới có thể xem.");
        }
        // 2. Coordinator , admin được xem ds này
        if (teamId == null) {
            throw new BadRequestException("Hãy cung cấp ID của TEAM để xem danh sách chi tiết của Team. ");
        }
        Integer finalExpertId = null;
        if (account.getRole() == AccountRole.EXPERT) {
            // Nếu là EventCoordinator, admin muốn xem thông tin expert quản lý phải nhập id tương ứng cuar họ
            Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin Chuyên gia tương ứng với tài khoản này."));
            finalExpertId = expert.getExpertId();
        }
        Team team;
        // Nếu là expert thì phải dùng account Expert của mình để xem thông tin Team mình quản lý
        if (account.getRole() == AccountRole.ADMIN || account.getRole() == AccountRole.EVENTCOORDINATOR) {            // Lấy thông tin expert
            team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đội thi có ID: " + teamId));
        } else {
            // Nếu là EXPERT, bắt buộc phải check xem team này có nằm trong vòng đấu ông ấy quản lý không
            team = teamRepository.findTeamByIdAndExpertAssignment(teamId, finalExpertId)
                    .orElseThrow(() -> new BadRequestException("Đội thi không tồn tại hoặc bạn không có quyền quản lý đội thi này."));
        }

        //3. Lấy danh sách teamMember thông qua Team
        List<TeamDetailResponse> list = new ArrayList<>();
        List<TeamDetailResponse.MemberInfo> memberList = new ArrayList<>();
        TeamDetailResponse.MemberInfo leaderInfo = null;
        if (team.getTeamMembers() != null && !team.getTeamMembers().isEmpty()) {

            for (TeamMember member : team.getTeamMembers()) {
                String avatar = (member.getStudent() != null && member.getStudent().getAccount() != null)
                        ? member.getStudent().getAccount().getAvatarUrl() : null;

                TeamDetailResponse.MemberInfo info = TeamDetailResponse.MemberInfo.builder()
                        .fullName(member.getStudent().getStudentName())
                        .university(member.getStudent().getUniversityName())
                        .major(member.getStudent().getMajor())
                        .avatarUrl(avatar)
                        .build();

                if (member.getIsLeader()) {
                    leaderInfo = info;
                } else {
                    memberList.add(info);
                }

            }

        }

        return TeamDetailResponse.builder()
                .teamId(team.getTeamId())
                .teamName(team.getTeamName())
                .leader(leaderInfo)
                .members(memberList)
                .build();

    }

    @Override
    public List<TeamDetailResponse> getTeamInfor(Integer expertId, CustomUserDetails userDetails) {
        //1. Check admin
        Account account = userDetails.getAccount();
        if (account.getRole() != AccountRole.ADMIN
                && account.getRole() != AccountRole.EXPERT
                && account.getRole() != AccountRole.EVENTCOORDINATOR) {
            throw new BadRequestException("Bạn không có quyền  xem danh sách này. Chỉ có ADMIN, EVENT COORDINATOR , EXPERT mới có thể xem.");
        }
        //1.2. Coordinator , admin được xem ds này
        Integer finalExpertId = null;
        if (account.getRole() == AccountRole.ADMIN || account.getRole() == AccountRole.EVENTCOORDINATOR) {
            // Nếu là EventCoordinator, admin muốn xem thông tin expert quản lý phải nhập id tương ứng cuar họ
            if (expertId == null) {
                throw new BadRequestException("Hãy cung cấp ID của expert để xem danh sách Team họ quản lý. ");
            }
            finalExpertId = expertId;
        }
        // Nếu là expert thì phải dùng account Expert của mình để xem thông tin Team mình quản lý
        else if (account.getRole() == AccountRole.EXPERT) {
            Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin Expert tương ứng với account này."));
            finalExpertId = expert.getExpertId();

        }

        //2. Lấy list team mà Expert quản lý
        List<Team> listTeam = teamRepository.findTeamsByExpertAssignment(finalExpertId);
        //3. Lấy danh sách teamMember thông qua Team
        List<TeamDetailResponse> list = new ArrayList<>();
        final Integer searchExpertId = finalExpertId;
        for (Team team : listTeam) {
            int count = team.getTeamSize();
            String categoryName = "N/A";
            String roundName = "N/A";

            if (team.getRegistrations() != null && !team.getRegistrations().isEmpty()) {
                for (Registration registration : team.getRegistrations()) {
                    Participant participant = registration.getParticipant();
                    if (participant != null && participant.getCategoryRound() != null) {
                        CategoryRound categoryRound = participant.getCategoryRound();

                        // Check Category Round này có đúng là cái mà Expert này được phân công không
                        boolean isTrue = categoryRound.getExpertAssigns() != null &&
                                categoryRound.getExpertAssigns().stream().anyMatch(expertAssign ->
                                        expertAssign.getExpert() != null
                                                && searchExpertId != null // Bảo đảm finalExpertId không null trước khi so sánh
                                                && expertAssign.getExpert().getExpertId() == searchExpertId
                                );
                        if (isTrue) {
                            if (categoryRound.getCategory() != null) {
                                categoryName = participant.getCategoryRound().getCategory().getCategoryName();

                            }
                            if (categoryRound.getRound() != null) {
                                roundName = participant.getCategoryRound().getRound().getRoundName();
                            }
                            break;
                        }
                    }

                }


            }
            TeamDetailResponse response = TeamDetailResponse.builder()
                    .teamId(team.getTeamId())
                    .teamName(team.getTeamName())
                    .sizeTeam(count)
                    .categoryName(categoryName)
                    .roundName(roundName)
                    .build();

            list.add(response);
        }

        return list;
    }


}

