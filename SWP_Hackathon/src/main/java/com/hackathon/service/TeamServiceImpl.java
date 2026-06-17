package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;

import com.hackathon.email.MailRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.NotificationStatus;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.entity.enums.TeamStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor

public class TeamServiceImpl implements TeamService {
    private final HackathonEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final AccountRepository accRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;
    private static final int MAX_TEAM_SIZE = 5;
    private static final long LOCK_BEFORE_DEADLINE_HOURS = 24;
    private static final long INVITATION_EXPIRE_HOURS = 3;
    private final StudentRepository studentRepository;
    private final EmailService emailService;


    //Kiểm tra thời gian đăng ký Team (check thời gian đóng cổng dk team,... trước 24h)
    @Override
    public HackathonEvent checkTeamRegistrationWindow(Integer eventId) {
        // Tim event
        // 1. Tìm tất cả các Sự kiện đang hoạt động trong hệ thống
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() ->
                new BadRequestException("Không tìm thấy sự kiện cuộc thi Hakathon"));

        // Check event co hoat dong khong
        if (event.getRegistrationDeadline() == null) {
            throw new BadRequestException("Sự kiện chưa cấu hình thời gian đăng ký!");
        }
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new BadRequestException("Sự kiện này hiện không trong trạng thái hoạt động.");
        }

        // Check if team registration window is still open (not locked 24h before deadline)
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline().minusHours(LOCK_BEFORE_DEADLINE_HOURS))) {
            throw new BadRequestException("Hệ thống đã khóa các thao tác liên quan đến Team( Tạo/ Mời/ Rời/ Phản hồi lời mời)." +
                    " Bạn không được phép thao tác bất kỳ thứ gì ngay thời điểm này." +
                    " Bạn chỉ được phép thực hiện các thao tác chỉnh sửa trước " + LOCK_BEFORE_DEADLINE_HOURS +
                    " ggiờ so với thời điểm chốt đăng ký.");
        }
        return event;
    }

    //FUNCTION 1:Create Team
    //BR: Khi tao team phai co tieu thieu it nhat 1 thanh vien duoc moi (bao gom leader va 1 thanh vien khac)
    //BR-07: Sau khi hết hạn đăng ký không được xóa hoặc thay đổi thành viên
    //BR-08: Lời mời/Tạo team phải thực hiện trước khi chốt danh sách 24 giờ

    @Transactional
    @Override
    public TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetail) {

        // 1. Check Deadline Registration createTeam(BR-7)
        if (request.getEventId() == null) {
            throw new BadRequestException("Không xác định được cuộc thi bạn muốn tạo đội.");
        }

        HackathonEvent event = checkTeamRegistrationWindow(request.getEventId());

        // 2. Lay thong tin cua leader(Nguoi tao tem se duoc gan role la leader)
        Account leaderAccount = userDetail.getAccount();

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
        if (cleanEmails.contains(leaderAccount.getEmail())) {
            throw new BadRequestException("Bạn là Trưởng nhóm, không cần tự mời chính mình!");
        }


        //3.4 Check người tạo Team ban đầu (account đnag tạo team)có tham gia team khác không
        List<TeamMember> existingTeams = teamMemberRepository.findByStudent(leaderAccount.getStudent());
        if (!existingTeams.isEmpty()) {
            // Duyệt qua danh sách để xem ông này có làm Leader của team nào trong số đó không
            boolean isLeaderOfAnyTeam = existingTeams.stream().anyMatch(TeamMember::getIsLeader);
            if (isLeaderOfAnyTeam) {
                throw new BadRequestException("Bạn đang là Leader của 1 team do đó bạn không thể tạo Team mới");
            } else {
                throw new BadRequestException("Bạn đã tham gia một đội khác với tư cách thành viên. Vui lòng rời đội cũ trước khi tự tạo đội mới!");
            }
        }

        //3.4 Check trùng tên Nhóm
        boolean existName = teamRepository.existsByTeamNameIgnoreCase(request.getTeamName().trim());
        if (existName) {
            throw new BadRequestException("Tên nhóm này đã được đăng ký trong cuộc thi này rồi!");
        }
//        // 3.5 Check xem Leader đã có team trong Event này chưa
//
//        boolean leaderAlreadyInEvent = teamMemberRepository.isStudentAlreadyInEvent(leaderAccount.getStudent().getStudentId(), event.getEventId());
//        if (leaderAlreadyInEvent) {
//            throw new BadRequestException("Bạn đã tham gia một đội khác trong cuộc thi này rồi!");
//        }
//        boolean leaderAlreadyInTeamForEvent = teamMemberRepository
//                .existsByStudentAndTeam_Event_EventId(leaderAccount.getStudent(), request.getEventId());
//
//        if (leaderAlreadyInTeamForEvent) {
//            throw new BadRequestException("Bạn đã tham gia hoặc tạo một đội khác trong cuộc thi này rồi!");
//        }


        //4. Create Team
        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setStatus(TeamStatus.DRAFT);
        team.setTeamSize(1);
        Team saveTeam = teamRepository.save(team);


        //4. Luu thong tin Leader
        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(saveTeam);
        leaderMember.setIsLeader(true);
        leaderMember.setStudent(leaderAccount.getStudent());
        teamMemberRepository.save(leaderMember);

        List<TeamResponse.MemberInfo> listMember = new ArrayList<>();
        List<String> invitedEmails = new ArrayList<>();
        //5. Tạo object, save info of leader vao ListMember
        TeamResponse.MemberInfo leaderInfo = new TeamResponse.MemberInfo(
                leaderAccount.getStudent().getStudentCode()
                , leaderAccount.getStudent().getStudentName()
                , leaderAccount.getEmail());
        listMember.add(leaderInfo);

        //6. Tao loi moi gui toi cac thah vien
        for (String memberEmail : cleanEmails) {
            Account memberAccount = accRepository.findByEmail(memberEmail.trim()).orElseThrow(() -> new BadRequestException("Member Account not found"));
            Notification invite = new Notification();
            invite.setAccount(memberAccount);
            invite.setTeam(saveTeam);
            invite.setEvent(event);
            invite.setType(NotificationType.TEAM_INVITATION);
            invite.setStatus(NotificationStatus.PENDING);
            invite.setTitle("INVITE TEAM " + saveTeam.getTeamName());
            invite.setMessage("Bạn được mời bởi " + leaderAccount.getStudent().getStudentName() +
                    " để tạo đội  tham gia cuộc thi Hackathon " + event.getEventName());
            Notification savedNoti = notificationRepository.save(invite);
            invitedEmails.add(memberEmail);

            //7. Gui loi moi den cac thnah vien
            // 2. Send Email per member
            try {
                MailRequest mailRequest = new MailRequest();
                mailRequest.setTo(memberAccount.getEmail());
                mailRequest.setSubject("FPT HACKATHON - Team Invitation: " + event.getEventName());

                Map<String, Object> props = new HashMap<>();
                props.put("studentName", memberAccount.getStudent().getStudentName());
                props.put("teamName", saveTeam.getTeamName());
                props.put("leaderName", leaderAccount.getStudent().getStudentName());
                props.put("email", leaderAccount.getEmail());
                props.put("receiverEmail", memberAccount.getEmail());
                props.put("notificationId", savedNoti.getId());

                mailRequest.setProps(props);

                emailService.sendEmail(mailRequest, "invitation");

            } catch (Exception e) {
                System.out.println("Email error: " + e.getMessage());
            }


        }
        //8. Return TeamResponse
        return new TeamResponse(saveTeam.getTeamId(), saveTeam.getTeamName(), leaderInfo, listMember, saveTeam.getCreateAt(), invitedEmails);
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
                .filter(TeamMember::getIsLeader)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Bạn chỉ là thành viên, không có quyền thay đổi thông tin đội!"));

        // Lấy ra đối tượng Team từ dòng Leader tìm được
        Team team = leaderRole.getTeam();

//        // 3. Check leader(Check account student đang login có phải là leader ko )
//        if (currentMember.getIsLeader() == null) {
//            throw new BadRequestException("Bạn không phải Trưởng nhóm, không có quyền đổi tên đội này!");
//        }

        // 4. Check Team này đã được phê duyệt đội chưa
        if (team.getStatus() == TeamStatus.APPROVED) {
            throw new BadRequestException("Bạn không được phép thay đổi tên nhóm khi đã gửi đơn đăng ký Team thành công.");
        }
        // 5.Check deadline(chi duoc update truoc khi het han dk tao team )
        HackathonEvent event = eventRepository.findByStatus(EventStatus.ACTIVE).getFirst();
        Optional<Registration> registration = registrationRepository.findByTeamAndHackathonEvent_EventId(team, event.getEventId());

        //6. Nếu đã nộp đơn đăng ký cuộc thi này rồi (PENDING hoặc APPROVED) thì khóa, không cho sửa tên nhóm
        if (registration.isPresent()) {
            Registration reg = registration.get();
            if (reg.getStatus() == TeamStatus.PENDING || reg.getStatus() == TeamStatus.APPROVED) {
                throw new BadRequestException("Đội đã nộp đơn đăng ký tham gia cuộc thi, không thể thay đổi tên nhóm vào lúc này!");
            }
        }
        checkTeamRegistrationWindow(event.getEventId());

        // 5. Validate team name
        if (teamName == null
                || teamName.isBlank()) {
            throw new BadRequestException(
                    "Tên đội mới không được để trống!");
        }
        String cleanName = teamName.trim();
        if (cleanName.equalsIgnoreCase(team.getTeamName())) {
            return team.getTeamName();
        }
        // 6. Update
        team.setTeamName(cleanName);
        teamRepository.save(team);
        return team.getTeamName();
    }

    //FUNCTION 3: RỜI TEAM
    //BR-03: Member chỉ được phép Leave team trước khi chốt danh sách 24 giờ

    @Transactional
    @Override
    public void leaveTeam(CustomUserDetails userDetails) {
        //1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        Account currentUser = userDetails.getAccount();
        Student student = currentUser.getStudent();
        //1.1 Check Student có đang thuộc Team nào không
        List<TeamMember> teamMemberList = teamMemberRepository.findByStudent(student);
        if (teamMemberList == null || teamMemberList.isEmpty()) {
            throw new BadRequestException("Bạn hiện không tham gia bất kỳ đội nào!");
        }

        if (teamMemberList.size() > 1) {
            throw new BadRequestException(
                    "Dữ liệu không hợp lệ: Một sinh viên đang thuộc nhiều Team.");
        }

        TeamMember teamMember = teamMemberList.getFirst();
        //2. Leader khong duoc phep roi khoi nhom , truoc khi chuyen quyen leader cho nguoi khac
        if (teamMember.getIsLeader()) {
            throw new BadRequestException("Leader không được phép rời Team trước khi chuyển quyền cho thành viên khác.");
        }

        Team team = teamMember.getTeam();
        // 4. Check Team này đã được phê duyệt đội chưa
        if (team.getStatus() == TeamStatus.APPROVED) {
            throw new BadRequestException("Bạn không được phép rời Team , bởi vì Team này đã đăng ký thông tin thành viên thành công với ban tổ chức rồi.");
        }

        //  5. Check thoi han duoc roi khoi team
        // check Team da dang ky event ch
        HackathonEvent event = eventRepository.findByStatus(EventStatus.ACTIVE).getFirst();
        Optional<Registration> registrationEvent = registrationRepository.findByTeamAndHackathonEvent_EventId(team, event.getEventId());

        //Th1: Neu team da gui don dang ky cuoc thi  roi, thi khong duoc phep roi Team
        if (registrationEvent.isPresent()) {
            Registration regis = registrationEvent.get();

            if (regis.getStatus() == TeamStatus.PENDING || regis.getStatus() == TeamStatus.APPROVED) {
                throw new BadRequestException("Team đã nộp đơn đăng ký, bạn không thể rời khỏi team .");
            }
            // TH2: Team có đơn nhưng trạng thái khác (ví dụ bị REJECTED ), vẫn phải check deadline của giải đó
            checkTeamRegistrationWindow(regis.getHackathonEvent().getEventId());
        }
        //Th3. Team ch gửi đơn đk , thi check thời hạn được phép rời khỏi Team
        checkTeamRegistrationWindow(event.getEventId());


        // Xoa
        teamMemberRepository.delete(teamMember);

        //5. Cập nhật lại số lượng thành viên thực tế trong DB
        team.setTeamSize(Math.max(0, team.getTeamSize() - 1));
        teamRepository.save(team);

    }


    //FUNCTION 4: CHUYỂN QUYỀN LEADER(Chỉ mới gửi lời mời đến thành viên muốn chuyển quyền )
    @Override
    public void transferLeader(TeamRequest request, CustomUserDetails userDetails) {

        // 2. Check Team
        Account currentUser = userDetails.getAccount();
        List<TeamMember> teamMemberList = teamMemberRepository.findByStudent(currentUser.getStudent());
        if (teamMemberList == null || teamMemberList.isEmpty()) {
            throw new BadRequestException("Không tìm thấy thông tin nhóm.Bạn hiện chưa tham gia bất kỳ Team nào.");
        }
        if (teamMemberList.size() > 1) {
            throw new BadRequestException(
                    "Dữ liệu không hợp lệ: Một sinh viên đang thuộc nhiều Team.");
        }


        TeamMember teamMember = teamMemberList.getFirst();

        //3. Check Team đã được phê duyệt chưa(xem lại bussiness rule)
        Team team = teamMember.getTeam();
        if (team.getStatus() == TeamStatus.APPROVED) {
            throw new BadRequestException("Team đã được phê duyệt bạn không được phép đổi Leader");
        }

        //4. Check Leader có thuộc Team ko
        if (!teamMember.getIsLeader()) {
            throw new BadRequestException("Chỉ Leader hiện tại mới được quyền chuyển quyền Trưởng nhóm.");
        }

        // 5.CheckDeadline
        List<HackathonEvent> eventList = eventRepository.findByStatus(EventStatus.ACTIVE);
        if (eventList == null || eventList.isEmpty()) {
            throw new BadRequestException("Không có event ACTIVE nào!");
        }
        HackathonEvent event = eventList.getFirst();
        Optional<Registration> registrationEvent = registrationRepository.findByTeamAndHackathonEvent_EventId(team, event.getEventId());
        checkTeamRegistrationWindow(event.getEventId());
        if (registrationEvent.isPresent()) {
            Registration registration = registrationEvent.get();
            if (registration.getStatus() == TeamStatus.PENDING || registration.getStatus() == TeamStatus.APPROVED) {
                throw new BadRequestException("Đội đã nộp đơn đăng ký tham gia cuộc thi, không thể chuyển quyền Leader vào lúc này!");
            }
        }

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
        inviteTransfer.setStatus(NotificationStatus.PENDING);
        inviteTransfer.setEvent(event);
        Notification savedNoti = notificationRepository.save(inviteTransfer);

        System.out.println(
                "Notification ID = " + savedNoti.getId());
        // 9. Gửi lời mời
        try {
            MailRequest mailRequest = new MailRequest();
            mailRequest.setTo(newLeader.getAccount().getEmail());
            mailRequest.setSubject("FPT HACKATHON. Lời mời chuyển quyền Leader cho cuộc thi " + event.getEventName() +
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
            mailRequest.setContent("");


        } catch (Exception e) {
            System.out.println("==> Lỗi gửi email chuyển quyền leader: " + e.getMessage());
        }
    }

    //FUNCTION 5: HÀM XỬ LÝ CHẤP NHẬN LỜI MỜI CHO TRANSFER, INVITE TEAM
    @Override
    public void acceptGeneralInvite(Long notificationId, CustomUserDetails userDetails) {
        //1.Tìm lời mời dựa trên thông báo
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại hoặc đã bị hủy từ trước."));
        ;
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
        if (notification.getStatus() == NotificationStatus.ACCEPTED
                || notification.getStatus() == NotificationStatus.REJECTED
                || notification.getStatus() == NotificationStatus.EXPIRED
                || notification.getStatus() == NotificationStatus.INVALID) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc không còn hiệu lực.");
        }
        // 4.1 Check trường hợp Team ko tồn tại
        Team team = notification.getTeam();
        if (team == null || team.getTeamSize() == null || team.getTeamSize() <= 0) {
            notification.setStatus(NotificationStatus.INVALID);
            notificationRepository.save(notification);
            throw new BadRequestException("Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
        }

//        5. Check thời hạn(ĐK: CÙNG BUSINESS RULE)
        if (notification.getEvent() != null) {
            checkTeamRegistrationWindow(notification.getEvent().getEventId());
        }

        //6.
        switch (notification.getType()) {
            case TEAM_INVITATION:
                this.acceptInvite(notification, userDetails);
//                notification.setStatus(NotificationStatus.ACCEPTED);
                break;

            case LEADER_TRANSFER_REQUEST:
                this.acceptLeaderTransfer(notification, userDetails);
//                notification.setStatus(NotificationStatus.ACCEPTED);
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
            notification.setStatus(NotificationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }


        // 5. Check người này đã cs tham gia team khác chưa (nếu rồi thì chặn)
        List<TeamMember> currentTeams = teamMemberRepository.findByStudent(inviteAccount.getStudent());
        if (currentTeams != null && !currentTeams.isEmpty()) {
            // Nếu trong DB, sinh viên này đã có dòng liên kết với 1 team nào đó rồi -> CHẶN NGAY
            throw new BadRequestException("Bạn đã tham gia một đội thi khác rồi, không thể gia nhập đội này.");
        }


        // 6. Check so luong thanh vien hien tai cua nhom
        int currentSize = Optional.ofNullable(team.getTeamSize()).orElse(0);
        if (currentSize >= MAX_TEAM_SIZE) {
            notification.setTitle("INVALID. Team đã đủ thành viên");
            notification.setMessage("Lời mời này không còn hiệu lực vì Đội thi đã đủ thành viên.");
            notification.setStatus(NotificationStatus.INVALID);
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
        notification.setStatus(NotificationStatus.ACCEPTED);
        notificationRepository.save(notification);

        // 11. Check All Team, neu du 5 thanh vien , vo hieu hoa loi moi con lai
        if (team.getTeamSize() == MAX_TEAM_SIZE) {
            List<Notification> otherInvites = notificationRepository.findByTeam(team);
            for (Notification oldNoti : otherInvites) {
                if (oldNoti.getId().equals(notification.getId())) {
                    continue;
                }
                // Vo hieu hoa loi moi con lai
                if (oldNoti.getType() == NotificationType.TEAM_INVITATION && oldNoti.getStatus() == NotificationStatus.PENDING) {
                    oldNoti.setStatus(NotificationStatus.INVALID);
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
            notification.setStatus(NotificationStatus.EXPIRED);
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
        notification.setStatus(NotificationStatus.ACCEPTED);

    }

    @Override
    public void rejectGeneralInvite(Long notificationId, CustomUserDetails userDetails) {
        //1.Tìm lời mời dựa trên thông báo
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại hoặc đã bị hủy từ trước."));
        ;
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
        if (notification.getStatus() == NotificationStatus.ACCEPTED
                || notification.getStatus() == NotificationStatus.REJECTED
                || notification.getStatus() == NotificationStatus.EXPIRED
                || notification.getStatus() == NotificationStatus.INVALID) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc không còn hiệu lực.");
        }
        // 4.1 Check trường hợp Team ko tồn tại
        Team team = notification.getTeam();
        if (team == null || team.getTeamSize() == null || team.getTeamSize() <= 0) {
            notification.setStatus(NotificationStatus.INVALID);
            notificationRepository.save(notification);
            throw new BadRequestException("Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
        }

//        5. Check thời hạn(ĐK: CÙNG BUSINESS RULE)
        if (notification.getEvent() != null) {
            checkTeamRegistrationWindow(notification.getEvent().getEventId());
        }

        //6.

        // Nếu bấm CHẤP NHẬN, chạy logic cũ của bạn
        switch (notification.getType()) {
            case TEAM_INVITATION:
                notification.setTitle("INVITATION REJECTED. Bạn đã từ chối vào Team: " + team.getTeamName());
                notification.setMessage("Yêu cầu gia nhập đội hình đã bị bạn từ chối.");
                break;

            case LEADER_TRANSFER_REQUEST:
                notification.setTitle("TRANSFER REJECTED. Bạn từ chối làm Leader Team: " + team.getTeamName());
                notification.setMessage("Yêu cầu nhận quyền Trưởng nhóm đã bị bạn từ chối.");
                break;

            default:
                throw new BadRequestException("Loại thông báo không hợp lệ để thực hiện thao tác từ chối.");
        }
//        notification.setStatus(NotificationStatus.REJECTED);
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
            notification.setStatus(NotificationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }
        Student newLeaderStudent = inviteAccount.getStudent();
        notification.setTitle("TRANSFER REJECTED. Tôi từ chối làm Leader Team: " + team.getTeamName());
        notification.setMessage("Bạn đã từ chối lời mời chuyển quyền Leader.");
        notification.setStatus(NotificationStatus.REJECTED);


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
        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        LocalDateTime expiredAt = notification.getCreatedAt().plusDays(3);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            notification.setTitle("EXPIRED. Lời mời tham gia : " + team.getTeamName() + " hết hạn.");
            notification.setMessage("Lời mời này có thời hạn trong vòng " + INVITATION_EXPIRE_HOURS + " ngày");
            notification.setStatus(NotificationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }
        Student newLeaderStudent = inviteAccount.getStudent();
        notification.setTitle("INVITE REJECTED. Tôi từ chối lời mời tham gia nhóm : " + team.getTeamName());
        notification.setMessage("Bạn đã từ chối lời mời tham gia nhóm.");
        notification.setStatus(NotificationStatus.REJECTED);

    }

    @Override
    public TeamResponse getTeamMember(Integer teamId, CustomUserDetails userDetails) {
        //1. Check team có tồn tại không
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        //2. Check account đang đăng nhập có đag  là thành viên của Team đó hay không
        Account currentAccount = userDetails.getAccount();
        TeamMember teamMember = teamMemberRepository.findByTeamAndStudent(team, currentAccount.getStudent())
                .orElseThrow(() -> new BadRequestException("Sinh viên hiện tại không thuộc Team này. Không được phép xem danh sách Team này."));
        //3. Lấy danh sách teamMember
        List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
        TeamResponse.MemberInfo leaderInfo = null;
        List<TeamResponse.MemberInfo> officialMembers = new ArrayList<>();

        for (TeamMember member : teamMembers) {
            TeamResponse.MemberInfo info = new TeamResponse.MemberInfo(
                    member.getStudent().getStudentCode(),
                    member.getStudent().getStudentName(),
                    member.getStudent().getAccount().getEmail()
            );

            // Phân loại dựa vào bảng trung gian TeamMember
            if (member.getIsLeader()) {
                leaderInfo = info;
            } else {
                officialMembers.add(info); // Chỉ add thành viên thường vào list này
            }
        }

        // 4. Lấy danh sách các email lời mời đang chờ (PENDING)
        List<Notification> pendingInvites = notificationRepository.findByTeamAndTypeAndStatus(team, NotificationType.TEAM_INVITATION, NotificationStatus.PENDING);
        List<String> pendingEmails = new ArrayList<>();
        for (Notification invite : pendingInvites) {
            pendingEmails.add(invite.getAccount().getEmail());
        }

        // 4. Đóng gói dữ liệu trả về cho Frontend
        return new TeamResponse(
                team.getTeamId(),
                team.getTeamName(),
                leaderInfo,
                officialMembers,
                team.getCreateAt(),
                pendingEmails
        );

    }

    @Override
    @Transactional
    public void registerEvent(CreateTeamRequest request, CustomUserDetails userDetails) {
        // Check thời hạn đăng ký cuộc thi
        HackathonEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về sự kiện này."));
        if (LocalDateTime.now().isBefore(event.getStartDate())) {
            throw new BadRequestException("Cuộc thi chưa mở cổng đăng ký! Vui lòng quay lại sau.");
        }
        if (LocalDateTime.now().isAfter(event.getEndDate())) {
            throw new BadRequestException("Đã quá hạn đăng ký tham gia cuộc thi này!");
        }

        //1. Check Team
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại. Bạn không được phép đăng ký event."));
        //2. Check leader
        Account currentAccount = userDetails.getAccount();
        TeamMember accountLeader = teamMemberRepository.findByTeamAndIsLeader(team, true)
                .orElseThrow(() -> new BadRequestException("Team chưa có leader"));
        if (accountLeader.getStudent().getStudentId()!=(currentAccount.getStudent().getStudentId())) {
            throw new BadRequestException("Bạn không phải là Leader, bạn không được phép đăng ký event");

        }
        //3. Check status hiện tại của Team(Draf, pending, approve)
        if (team.getStatus().equals(TeamStatus.PENDING)) {
            throw new BadRequestException("Đội của bạn đã gửi đơn đăng ký trước đó rồi.Xin hãy chờ ban tổ chức phê duyệt");
        }
        if (team.getStatus().equals(TeamStatus.APPROVED)) {
            throw new BadRequestException("Đội của bạn đã được phê duyệt đơn đăng ký rồi.");
        }
        if (team.getStatus().equals(TeamStatus.REJECTED)) {
            throw new BadRequestException("Đội của bạn đã bị từ chối.Vui lòng kiểm tra lại thông tin đăng ký.");
        }
        //4.Check số lượng thành viên
        int countMember = team.getTeamSize();
        if (countMember < event.getMinTeamSize()) {
            throw new BadRequestException("Bạn không thể đăng ký cuộc thi. Số lượng thành viên tối thiểu bắt buộc phải lớn hơn hoặc bằng " + event.getMinTeamSize() +
                    " .Thành viên chính thức hiện tại bạn đang sở hữu là " + countMember);
        }
        if (countMember > event.getMaxTeamSize()) {
            throw new BadRequestException("Bạn không thể đăng ký cuộc thi. Số lượng thành viên tối đa của cuộc thi này là: " + event.getMaxTeamSize() +
                    " .Thành viên chính thức hiện tại bạn đang sở hữu là " + countMember);
        }

        //5. Check xem này đã từng đăng ký cuộc thi này chưa
        Optional<Registration> registrationEvent =
                registrationRepository.findByTeamAndHackathonEvent_EventId(team, event.getEventId());

        if (registrationEvent.isPresent()) {
            throw new BadRequestException("Team đã đăng ký rồi");
        }

        // 5. Tạo bảng registration để lưu thông tin đăng ký
        Registration registration = new Registration();
        registration.setHackathonEvent(event);
        registration.setTeam(team);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setStatus(TeamStatus.PENDING);
        registrationRepository.save(registration);

        team.setStatus(TeamStatus.PENDING);
        teamRepository.save(team);


    }

}

