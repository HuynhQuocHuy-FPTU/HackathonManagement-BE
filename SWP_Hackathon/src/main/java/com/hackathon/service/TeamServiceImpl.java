package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.TeamStatus;
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
    private final HackathonEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final AccountRepository accRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;
    private static final int MAX_TEAM_SIZE = 5;
    private static final long LOCK_BEFORE_DEADLINE_HOURS = 24;


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
        HackathonEvent event = checkTeamRegistrationWindow(request.getEventId());

        // 2. Lay thong tin cua leader(Nguoi tao tem se duoc gan role la leader)
        Account leaderAccount = userDetail.getAccount();
        if (request.getMemberEmails() == null) {
            throw new BadRequestException("Member email list is required");
        }

        // 2.1 BR: bắt buộc mời ít nhất 1 người khác
        if (request.getMemberEmails() == null || request.getMemberEmails().isEmpty()) {
            throw new BadRequestException("Khi tạo đội, bạn bắt buộc phải mời ít nhất 1 thành viên khác tham gia!");
        }

        //2.2. Check xem account này có tham gia team khác không
        boolean alreadyInTeam =
                teamMemberRepository.existsByStudent(
                        leaderAccount.getStudent());

        if (alreadyInTeam) {
            throw new BadRequestException(
                    "Bạn đã thuộc một team khác!");
        }

        // 2.3  Check duplicate member and loc email
        Set<String> cleanEmails = new HashSet<>();
        for (String email : request.getMemberEmails()) {
            if (email != null && !email.isBlank()) {
                cleanEmails.add(email.trim());
            }
        }

        if (cleanEmails.isEmpty()) {
            throw new BadRequestException("Danh sách email mời vào nhóm không hợp lệ!");
        }

        //2.5 Check Email cua Leader vs Email cua listMember
        if (cleanEmails.contains(leaderAccount.getEmail().toLowerCase())) {
            throw new BadRequestException("Bạn là Trưởng nhóm, không cần tự mời chính mình!");
        }

        //3. Create Team
        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setStatus(TeamStatus.PENDING);
        team.setTeamSize(1);
        Team saveTeam = teamRepository.save(team);
        List<TeamResponse.MemberInfo> listMember = new ArrayList<>();
        List<String> invitedEmails = new ArrayList<>();

        //4. Luu thong tin Leader
        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(saveTeam);
        leaderMember.setIsLeader(true);
        leaderMember.setStudent(leaderAccount.getStudent());
        teamMemberRepository.save(leaderMember);

        //5. Tạo object, save info of leader vao ListMember
        TeamResponse.MemberInfo leaderInfo = new TeamResponse.MemberInfo(
                leaderAccount.getStudent().getStudentCode()
                , leaderAccount.getStudent().getStudentName()
                , leaderAccount.getStudent().getAccount().getEmail());
        listMember.add(leaderInfo);

        //6. Tao loi moi gui toi cac thah vien
        for (String memberEmail : cleanEmails) {
            Account memberAccount = accRepository.findByEmail(memberEmail.trim()).orElseThrow(() -> new BadRequestException("Member Account not found"));
            Notification invite = new Notification();
            invite.setAccount(memberAccount);
            invite.setTeam(saveTeam);
            invite.setEvent(event);// Thông tin này là thông tin ẩn để lưu team này được tạo bởi event nào
            invite.setTitle("Lời mời tham gia Team " + saveTeam.getTeamName());
            invite.setMessage("Bạn được mời bởi " + leaderAccount.getStudent().getStudentName() +
                    " để tạo đội cùng tham gia cuộc thi Hackathon.");
            notificationRepository.save(invite);
            invitedEmails.add(memberEmail);
        }
        //8. Return TeamResponse
        return new TeamResponse(saveTeam.getTeamId(), saveTeam.getTeamName(), leaderInfo, listMember, saveTeam.getCreateAt(), invitedEmails);
    }


    //FUNCTION 2:UPDATE INFORMATION ABOUT TEAM AS NAME
    @Override
    @Transactional
    public void updateInfo(CreateTeamRequest request, CustomUserDetails userDetails) {
        // 1.Check deadline(chi duoc update truoc khi het han dk tao team )
        HackathonEvent event = checkTeamRegistrationWindow(request.getEventId());

        // 2. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        Account currentAccount = userDetails.getAccount();

        // 3. Check leader(Check account student đang login có phải là leader ko )
        TeamMember leaderMember = teamMemberRepository.findByStudentAndIsLeader(currentAccount.getStudent(), true)
                .orElseThrow(() -> new BadRequestException("Chỉ Leader mới có quyền cập nhật thông tin của nhóm."));
        if (!leaderMember.getStudent().getStudentCode().equals(currentAccount.getStudent().getStudentCode())) {
            throw new BadRequestException("Bạn không phải Trưởng nhóm, không có quyền đổi tên đội này!");
        }
        // 4. Get team
        Team team = leaderMember.getTeam();
        // 5. Validate team name
        if (request.getTeamName() == null
                || request.getTeamName().isBlank()) {
            throw new BadRequestException(
                    "Tên đội mới không hợp lệ!");
        }

        String cleanName = request.getTeamName().trim();

        if (cleanName.equalsIgnoreCase(team.getTeamName())) {
            return;
        }

        // 6. Update
        team.setTeamName(cleanName);
        teamRepository.save(team);

    }

    //FUNCTION 3: RỜI TEAM
    //BR-03: Member chỉ được phép Leave team trước khi chốt danh sách 24 giờ
    @Override
    public void leaveTeam(TeamRequest request, CustomUserDetails userDetails) {
        //1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        Account currentUser = userDetails.getAccount();
        Team team = teamRepository.findById(request.getTeamId()).orElseThrow(() -> new RuntimeException("Team not found"));

        //2. Check Student co thuoc team ko or con trong nhom ko
        TeamMember teamMember = teamMemberRepository.findByTeamAndStudent(request.getTeamId(), currentUser.getStudent())
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên chính thức ủa Team này."));

        //3. Leader khong duoc phep roi khoi nhom , truoc khi chuyen quyen leader cho nguoi khac
        if (teamMember.getIsLeader()) {
            throw new BadRequestException("Leader không được phép rời Team trước khi chuyển quyền cho thành viên khác.");
        }

        // 4. Check Team này đã được phê duyệt đội chưa
        if (team.getStatus() == TeamStatus.APPROVED) {
            throw new BadRequestException("Bạn không được phép rời Team , bởi vì Team này đã đăng ký thông tin thành viên thành công với ban tổ chức rồi.");
        }

        //  5. Check thoi han duoc roi khoi team
        // check Team da dang ky event ch

        Optional<Registration> registration = registrationRepository.findByTeamAndHackathonEvent_EventId(team, request.getEventId());

        //Th1: Neu team da gui don dang ky cuoc thi  roi, thi khong duoc phep roi Team
        if (registration.isPresent()) {
            Registration registrationList = registration.get();
            if (registrationList.getStatus() == TeamStatus.PENDING || registrationList.getStatus() == TeamStatus.APPROVED) {
                throw new BadRequestException("Team đã nộp đơn đăng ký, bạn không thể rời khỏi team .");
            } else {
                //Th2. Team ch gửi đơn đk , thi check thời hạn được phép rời khỏi Team
                checkTeamRegistrationWindow(request.getEventId());
            }

        }

        // Xoa
        teamMemberRepository.delete(teamMember);

        //5. Cập nhật lại số lượng thành viên của nhóm
        team.setTeamSize(team.getTeamSize() - 1);
        teamRepository.save(team);
    }

    @Override
    public void transferLeader(TeamRequest request, CustomUserDetails userDetails) {
        // 1.CheckDeadline
        checkTeamRegistrationWindow(request.getEventId());
    }

    //FUNCTION 4: CHUYỂN QUYỀN LEADER


    //FUNCTION 5: CHẤP NHẬN LỜI MỜI
    // Chuyen quyen leader
    @Override
    @Transactional(dontRollbackOn = BadRequestException.class) // Khong roll Back khi dinh loi xu ly tb hong
    public void acceptInvite(Integer teamId, Long notificationId, CustomUserDetails userDetails) {
        //1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        Account currentUser = userDetails.getAccount();

        //2. Kiem tra team gui loi moi con ton tai khong
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new BadRequestException("Team does not exits"));

        //3. Tim thong bao or loi moi tuong ung
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new BadRequestException("No found Invitication"));

        //4. Check loi moi co thuoc ve nguoi dung hien tai khong
        if (notification.getAccount().getAccountId() != currentUser.getAccountId()) {
            throw new BadRequestException("You do not have the authority to process this invitation");
        }

        // 4.1 Danh dau thong bao da duoc doc
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        // 4.2 Check trang thai da xu ly chua
        String title = notification.getTitle();
        if (title != null && (title.contains("ACCEPTED") || title.contains("EXPIRED") || title.contains("INVALID"))) {
            throw new BadRequestException(
                    "This invitation has already been processed.");
        }

        // 5.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao
        LocalDateTime expiredAt = notification.getCreatedAt().plusDays(3);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            notification.setTitle("INVITATION EXPIRED. Invitation to join: " + team.getTeamName());
            notification.setMessage("This invitation has expired after 3 days and is no longer valid");
            notification.setRead(true);
            notificationRepository.save(notification);
            throw new BadRequestException("Your invitation has expired");
        }

        // 6. Check so luong thanh vien hien tai cua nhom
        int currentSize = Optional.ofNullable(team.getTeamSize()).orElse(0);
        if (currentSize >= 5) {

            notification.setTitle("INVALID. Team Invitation to " + team.getTeamName());
            notification.setMessage("This invitation is no longer valid because the team is full.");
            notification.setRead(true);
            notificationRepository.save(notification);
            throw new BadRequestException("Team '" + team.getTeamName() + "' has reached the maximum size.");
        }

        // 7. Check ng dung da la thanh vien cua nhom hay chua
        boolean exists = teamMemberRepository.existsByTeamAndStudent(team, currentUser.getStudent());
        if (exists) {
            throw new BadRequestException("You are already a member of this group");
        }

        // 8. Them vao team
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setIsLeader(false);
        member.setStudent(currentUser.getStudent());
        teamMemberRepository.save(member);

        // 9. Cap nhat so luong thanh vien
        team.setTeamSize(Optional.ofNullable(team.getTeamSize()).orElse(0) + 1);
        teamRepository.save(team);

        // 10. Cap nhat thong boa khi ban Chap nhan loi moi
        notification.setTitle("INVITATION ACCEPTED. You have joined " + team.getTeamName());
        notification.setMessage("You are now an official member of team " + team.getTeamName());
        notification.setRead(true);
        notificationRepository.save(notification);

        // 11. Check All Team, neu du 5 thanh vien , vo hieu hoa loi moi con lai
        if (team.getTeamSize() == 5) {
            List<Notification> otherInvites = notificationRepository.findByTeam(team);
            // Bỏ qua không cập nhật đè lên thông báo thành công của chính mình vừa xử lý ở trên
            for (Notification oldNoti : otherInvites) {
                if (oldNoti.getId().equals(notification.getId())) {
                    continue;
                }
                // Vo hieu hoa loi moi con lai
                String oldTitle = oldNoti.getTitle();
                if (oldTitle != null
                        && !oldTitle.contains("ACCEPTED")
                        && !oldTitle.contains("EXPIRED")
                        && !oldTitle.contains("INVALID")) {
                    oldNoti.setTitle("INVITATION INVALID. Invitation to Join " + team.getTeamName());
                    oldNoti.setMessage("This invitation is no longer valid because the team has reached its maximum capacity.");
                    oldNoti.setRead(true);
                    notificationRepository.save(oldNoti);
                }

            }
        }

    }


}
