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
    private final StudentRepository studentRepository;
    private final EmailService emailService;

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
        // Check thời gian mở cổng đăng ký Team và Cuộc Thi
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
        List<HackathonEvent> eventList = eventRepository.findByStatus(EventStatus.ACTIVE);
        if (eventList == null || eventList.isEmpty()) {
            throw new BadRequestException("Không có event ACTIVE nào!");
        }
        HackathonEvent event = eventList.getFirst();
        HackathonEvent checkDeadline = checkTeamRegistrationWindow(event.getEventId());

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

        //3.3 Check Email cua Leader vs Email cua listMember
        if (cleanEmails.contains(leaderAccount.getEmail())) {
            throw new BadRequestException("Bạn là Trưởng nhóm, không cần tự mời chính mình!");
        }


        //3.1 Check xem account này có tham gia team khác không
        boolean alreadyInTeam =
                teamMemberRepository.existsByStudent(
                        leaderAccount.getStudent());
        if (alreadyInTeam) {
            throw new BadRequestException("Bạn đã thuộc một team khác!");
        }
        //


        //4. Create Team
        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setStatus(TeamStatus.PENDING);
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
                System.err.println("Email error: " + e.getMessage());
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
        TeamMember currentMember = teamMemberRepository.findByStudent(currentAccount.getStudent())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin nhóm! Bạn hiện chưa tham gia bất kỳ Team nào."));

        // 3. Check leader(Check account student đang login có phải là leader ko )
        if (!currentMember.getIsLeader()) {
            throw new BadRequestException("Bạn không phải Trưởng nhóm, không có quyền đổi tên đội này!");
        }

        // 4. Check Team này đã được phê duyệt đội chưa
        Team team = currentMember.getTeam();
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
        TeamMember teamMember = teamMemberRepository.findByStudent(student)
                .orElseThrow(() -> new BadRequestException("Bạn hiện không tham gia bất kỳ đội nào!"));

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
            Registration registration = registrationEvent.get();
            if (registration.getStatus() == TeamStatus.PENDING || registration.getStatus() == TeamStatus.APPROVED) {
                throw new BadRequestException("Team đã nộp đơn đăng ký, bạn không thể rời khỏi team .");
            }

        }
        //Th2. Team ch gửi đơn đk , thi check thời hạn được phép rời khỏi Team
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
        TeamMember teamMember = teamMemberRepository.findByStudent(currentUser.getStudent())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin nhóm.Bạn hiện chưa tham gia bất kỳ Team nào."));

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
            System.err.println("==> Lỗi gửi email chuyển quyền leader: " + e.getMessage());
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
        if (userDetails != null) {
            if (notification.getAccount().getAccountId() != (userDetails.getAccount().getAccountId())) {
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
        //5. Check thời hạn(ĐK: CÙNG BUSINESS RULE)

        //6.
        switch (notification.getType()) {
            case TEAM_INVITATION:
                this.acceptInvite(notification, userDetails);
                break;

            case LEADER_TRANSFER_REQUEST:
                this.acceptLeaderTransfer(notification);
                break;

            default:
                throw new BadRequestException("Loại thông báo không hợp lệ.");
        }

        notification.setStatus(NotificationStatus.ACCEPTED);
        notificationRepository.save(notification);

    }

    @Override
    public void acceptLeaderTransfer(Notification notification) {

    }

    //FUNCTION 5: CHẤP NHẬN LỜI MỜI THAM GIA TEAM
    @Override
    @Transactional(dontRollbackOn = BadRequestException.class) // Khong roll Back khi dinh loi xu ly tb hong
    public void acceptInvite(Notification notification, CustomUserDetails userDetails) {
        //1. Tim thong bao or loi moi tuong ung
        //2. Kiem tra team gui loi moi con ton tai khong
        Team team = teamRepository.findById(notification.getTeam().getTeamId()).orElseThrow(() -> new BadRequestException("Team does not exits"));
        //3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Account inviteAccount = notification.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }

        // 4.1 Danh dau thong bao da duoc doc
        if (!notification.isRead()) {
            notification.setRead(true);
        }

        // 5.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        LocalDateTime expiredAt = notification.getCreatedAt().plusDays(3);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            notification.setTitle("EXPIRED. Lời mời tham gia : " + team.getTeamName() + " hết hạn.");
            notification.setMessage("This invitation has expired after 3 days and is no longer valid");
            notification.setStatus(NotificationStatus.EXPIRED);
            notificationRepository.save(notification);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
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
        notificationRepository.save(notification);

        // 11. Check All Team, neu du 5 thanh vien , vo hieu hoa loi moi con lai
        if (team.getTeamSize() == MAX_TEAM_SIZE) {
            List<Notification> otherInvites = notificationRepository.findByTeam(team);
            for (Notification oldNoti : otherInvites) {
                if (oldNoti.getId().equals(notification.getId())) {
                    continue;
                }
                // Vo hieu hoa loi moi con lai
                if (oldNoti.getStatus() == NotificationStatus.PENDING) {
                    oldNoti.setStatus(NotificationStatus.INVALID);
                    oldNoti.setTitle("INVALID. Lời mời vào đội " + team.getTeamName());
                    oldNoti.setMessage("This invitation is no longer valid because the team has reached its maximum capacity.");
                    oldNoti.setRead(true);
                    notificationRepository.save(oldNoti);
                }

            }
        }

    }


}

