package com.hackathon.service.impl;

import com.hackathon.dto.team.*;

import com.hackathon.email.MailRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.EmailService;
import com.hackathon.service.NotificationService;
import com.hackathon.service.SystemConfigService;
import com.hackathon.service.TeamService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

// Quản lý vòng đời đội thi, lời mời, yêu cầu tham gia và quyền trưởng nhóm.
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final AccountRepository accRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final ExpertRepository expertRepository;
    private final HackathonEventRepository hackathonEventRepository;
    private final SystemConfigService systemConfigService;
    private final TeamInvitationRepository teamInvitationRepository;

    private final EmailService emailService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TeamDraftRepository teamDraftRepository;

    @Override
    @Transactional(readOnly = true)
    // Lấy các đội đang hoạt động và còn khả năng tiếp nhận thành viên.
    public List<TeamActiveResponse> getActiveTeams() {
        int maxTeamSize = systemConfigService.getIntConfig(SystemConfigKey.MAX_TEAM_SIZE);
        return teamRepository
                .findByStatusAndTeamSizeLessThanOrderByCreateAtDesc(
                        TeamStatus.ACTIVE,
                        maxTeamSize)
                .stream()
                .map(team -> mapToActiveResponse(team, maxTeamSize))
                .toList();
    }

    @Override
    @Transactional
    // Gửi yêu cầu tham gia đội sau khi kiểm tra sinh viên và tình trạng của đội
    // đích.
    public TeamJoinResponse sendJoinRequest(Integer teamId, CustomUserDetails userDetails, TeamJoinRequest request) {
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Bạn chưa đăng nhập.");
        }
        Account account = userDetails.getAccount();

        Student student = account.getStudent();

        if (student == null) {
            throw new BadRequestException("Bạn không phải là sinh viên");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy team này"));

        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new BadRequestException(
                    "Team này hiện không nhận thêm thành viên.");
        }
        boolean alreadyMember = teamMemberRepository
                .findByTeamAndStudent(team, student)
                .isPresent();

        if (alreadyMember) {
            throw new BadRequestException(
                    "Bạn đã là thành viên của team này.");
        }

        validateStudentAvailability(student, null);

        boolean pendingRequestExists = teamInvitationRepository
                .existsByTeamAndAccountAndStatusAndType(
                        team,
                        account,
                        InvitationStatus.PENDING,
                        InvitationType.JOIN_REQUEST);

        if (pendingRequestExists) {
            throw new BadRequestException(
                    "Bạn đã gửi yêu cầu tham gia team này.");
        }

        int maxTeamSize = systemConfigService.getIntConfig(SystemConfigKey.MAX_TEAM_SIZE);

        if (team.getTeamMembers().size() >= maxTeamSize) {
            throw new BadRequestException("Số lượng thành viên đã đủ tối đa mà hệ thống cho phép");
        }
        TeamInvitation teamInvitation = new TeamInvitation();
        teamInvitation.setTeam(team);
        teamInvitation.setAccount(account);
        teamInvitation.setEmail(account.getEmail());
        teamInvitation.setType(InvitationType.JOIN_REQUEST);
        teamInvitation.setStatus(InvitationStatus.PENDING);
        teamInvitation.setReason(request.reason().trim());
        teamInvitation.setCreatedAt(LocalDateTime.now());

        TeamInvitation savedRequest = teamInvitationRepository.save(teamInvitation);

        return mapToTeamJoinResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    // Lấy các yêu cầu tham gia đang chờ trưởng nhóm xử lý theo trạng thái yêu cầu.
    public List<TeamJoinResponse> getPendingJoinRequests(CustomUserDetails userDetails, InvitationStatus status) {
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Bạn chưa đăng nhập.");
        }
        Account account = userDetails.getAccount();

        Student student = account.getStudent();

        if (student == null) {
            throw new BadRequestException("Bạn không phải là sinh viên");
        }
        Team team = getActiveTeamLedBy(student);

        List<TeamJoinResponse> list = teamInvitationRepository
                .findByTypeAndStatusAndTeam(InvitationType.JOIN_REQUEST, status, team).stream()
                .map(this::mapToTeamJoinResponse).toList();

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    // Lấy các yêu cầu hoặc lời mời tham gia đội dành cho sinh viên hiện tại.
    public List<TeamJoinResponse> getTeamJoinRequestForMember(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Bạn chưa đăng nhập.");
        }
        Account account = userDetails.getAccount();

        Student student = account.getStudent();

        if (student == null) {
            throw new BadRequestException("Bạn không phải là sinh viên");
        }
        List<TeamJoinResponse> list = teamInvitationRepository
                .findByAccountAndTypeOrderByCreatedAtDesc(
                        account,
                        InvitationType.JOIN_REQUEST)
                .stream()
                .map(this::mapToTeamJoinResponse)
                .toList();
        return list;
    }

    @Override
    @Transactional
    // Cho trưởng nhóm chấp nhận yêu cầu và thêm sinh viên vào đội nếu vẫn còn chỗ.
    public void acceptJoinRequest(Long requestId, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Bạn chưa đăng nhập.");
        }
        Account account = userDetails.getAccount();

        Student student = account.getStudent();

        if (student == null) {
            throw new BadRequestException("Bạn không phải là sinh viên");
        }
        TeamInvitation teamInvitation = teamInvitationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tham gia vào đội"));
        validatePendingJoinRequest(teamInvitation);

        Team team = teamInvitation.getTeam();
        validateTeamLeader(team, student);

        int maxTeamSize = systemConfigService.getIntConfig(SystemConfigKey.MAX_TEAM_SIZE);

        if (team.getTeamMembers().size() >= maxTeamSize) {
            throw new BadRequestException("Số lượng thành viên trong đội đã đạt tối đa");
        }

        Student requestingStudent = teamInvitation.getAccount().getStudent();
        if (requestingStudent == null) {
            throw new BadRequestException("Sinh viên gửi yêu cầu không hợp lệ");
        }

        if (teamMemberRepository.findByTeamAndStudent(team, requestingStudent).isPresent()) {
            throw new BadRequestException("Sinh viên này đã là thành viên của team");
        }

        validateStudentAvailability(requestingStudent, null);

        TeamMember newMember = TeamMember.builder()
                .team(team)
                .student(requestingStudent)
                .isLeader(false)
                .build();
        teamMemberRepository.save(newMember);

        int currentTeamSize = team.getTeamSize() == null
                ? team.getTeamMembers().size()
                : team.getTeamSize();
        team.setTeamSize(currentTeamSize + 1);
        teamRepository.save(team);

        teamInvitation.setStatus(InvitationStatus.ACCEPTED);
        teamInvitationRepository.save(teamInvitation);
    }

    @Override
    @Transactional
    // Từ chối yêu cầu tham gia đội và cập nhật trạng thái phản hồi.
    public void rejectJoinRequest(Long requestId, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Bạn chưa đăng nhập.");
        }
        Account account = userDetails.getAccount();

        Student student = account.getStudent();

        if (student == null) {
            throw new BadRequestException("Bạn không phải là sinh viên");
        }
        TeamInvitation teamInvitation = teamInvitationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tham gia vào đội"));
        validatePendingJoinRequest(teamInvitation);
        validateTeamLeader(teamInvitation.getTeam(), student);

        teamInvitation.setStatus(InvitationStatus.REJECTED);

        teamInvitationRepository.save(teamInvitation);
    }

    private TeamActiveResponse mapToActiveResponse(Team team, Integer maxTeamSize) {
        String leaderName = team.getTeamMembers().stream()
                .filter(member -> Boolean.TRUE.equals(member.getIsLeader()))
                .findFirst()
                .map(member -> member.getStudent().getStudentName())
                .orElse(null);

        List<String> memberNames = team.getTeamMembers().stream()
                .filter(member -> !Boolean.TRUE.equals(member.getIsLeader()))
                .map(member -> member.getStudent().getStudentName())
                .toList();

        return new TeamActiveResponse(
                team.getTeamId(), team.getTeamName(), leaderName, memberNames,
                team.getCreateAt(), maxTeamSize);
    }

    // Nếu Đội đã nộp đơn và thời gian hiện tại cách thời gian đk event dưới 24 giờ
    // -> CHẶN
    // Check đơn đăng ký và thời hạn 24h của Team dựa trên Event đã gửi đơn.
    // Nếu như chưa dk thì có quyền thay đổi tùy thích
    // Kiểm tra đội có đang chịu ràng buộc bởi thời gian đăng ký của sự kiện hay
    // không.
    public void checkEventRegistrationWindow(Team team) {
        List<Registration> registrations = registrationRepository.findByTeam(team);
        if (registrations == null || registrations.isEmpty()) {
            return; // Không có đăng ký sự kiện nào thì bỏ qua
        }
        int lockHours = systemConfigService.getIntConfig(SystemConfigKey.LOCK_BEFORE_DEADLINE_HOURS);
        LocalDateTime now = LocalDateTime.now();
        for (Registration regis : registrations) {
            HackathonEvent event = regis.getHackathonEvent();

            // 1. Nếu đơn đã APPROVED và sự kiện đã COMPLETED -> Chặn
            if (regis.getStatus() == RegistrationStatus.APPROVED && event != null
                    && event.getStatus() == EventStatus.COMPLETED && team.getStatus().equals(TeamStatus.BUSY)) {
                throw new BadRequestException("Cuộc thi đã hoàn thành, không thể thay đổi thông tin.");
            }

            // 2. Kiểm tra thời gian khóa sổ cho đơn PENDING (hoặc các trạng thái cần check
            // deadline)
            if (regis.getStatus() == RegistrationStatus.PENDING && event != null
                    && event.getRegistrationDeadline() != null) {

                // Tính thời điểm bắt đầu khóa sổ (Deadline trừ đi số giờ lock)
                LocalDateTime lockTime = event.getRegistrationDeadline().minusHours(lockHours);

                // NẾU thời gian hiện tại ĐÃ SAU (hoặc bằng) thời điểm khóa sổ -> Chặn!
                if (now.isAfter(lockTime) || now.isEqual(lockTime)) {
                    throw new BadRequestException(
                            "Hệ thống đã đóng cổng thay đổi thông tin do cuộc thi đã bước vào giai đoạn chốt sổ (Trước deadline "
                                    + lockHours + " giờ).");
                }
            }
        }

    }

    // Hàm kiểm tra lịch sử tham gia sự kiện và trạng thái bận của sinh viên
    private void validateStudentAvailability(Student student, Team targetTeam) {
        List<TeamMember> userCurrentTeams = teamMemberRepository.findByStudent(student);
        if (userCurrentTeams == null || userCurrentTeams.isEmpty()) {
            return;
        }

        List<Integer> targetEventIds;
        if (targetTeam != null) {
            List<Registration> registrations = registrationRepository.findByTeam(targetTeam);
            targetEventIds = registrations.stream().map(r -> r.getHackathonEvent().getEventId()).toList();
        } else {
            targetEventIds = new ArrayList<>();
        }

        for (TeamMember tm : userCurrentTeams) {
            if (tm.getTeam().getStatus() != TeamStatus.FINISHED) {
                if (tm.getTeam().getStatus() == TeamStatus.BUSY) {
                    throw new BadRequestException(
                            "Bạn đang trong trạng thái thi đấu ở một đội hình khác, không thể gia nhập đội này!");
                }
                if (tm.getTeam().getStatus() == TeamStatus.ACTIVE) {
                    throw new BadRequestException(
                            "Bạn đang là thành viên của mội đội khác, không thể tham gia đội này!");
                }
                if (tm.getTeam().getStatus() == TeamStatus.PENDING) {
                    throw new BadRequestException(
                            "Bạn đang là thành viên của một đội khác đang chờ xét duyệt, không thể tham gia đội này!");
                }

                if (!targetEventIds.isEmpty()) {
                    List<Registration> currentTeamRegis = registrationRepository.findByTeam(tm.getTeam());
                    boolean isDuplicatedEvent = currentTeamRegis.stream()
                            .anyMatch(r -> targetEventIds.contains(r.getHackathonEvent().getEventId()));
                    if (isDuplicatedEvent) {
                        throw new BadRequestException(
                                "Bạn đã tham gia một đội thi khác trong cùng cuộc thi này rồi, không thể gia nhập thêm!");
                    }
                }
            }
        }

    }

    // -------------------------------------//
    // STUDENT: TẠO TEAM, LỜI MỜI
    // -------------------------------------//

    // FUNCTION 1:Create Team
    // BR: Khi tao team phai co tieu thieu it nhat 1 thanh vien duoc moi (bao gom
    // leader va 1 thanh vien khac)

    @Transactional
    @Override
    // Tạo đội mới, chỉ định người tạo làm trưởng nhóm và xử lý danh sách thành viên
    // ban đầu.
    public TeamResponse createTeam(CreateTeamRequest request, @NonNull CustomUserDetails userDetail) {
        // 1. Lay thong tin cua account dang login (Nguoi tao tem se duoc gan role la
        // leader)
        Account leaderAccount = userDetail.getAccount();
        // 2. Check account đang tạo team có tham gia team khác không
        // 2.1 Nếu account này thuộc Team khác mà có trạng thái DRAFT || BUSY thì ko
        // được phép tạo team mới
        // 2.2 Chỉ được tạo Team mới, tham gia team mới khi team cũ có trạng thai FINISH
        List<TeamMember> existingTeams = teamMemberRepository.findByStudent(leaderAccount.getStudent());
        if (!existingTeams.isEmpty()) {
            for (TeamMember teamMember : existingTeams) {
                TeamStatus teamStatus = teamMember.getTeam().getStatus();

                if (teamStatus != TeamStatus.FINISHED) {
                    if (teamMember.getIsLeader()) {
                        throw new BadRequestException(
                                "Bạn đang là Leader  của một đội đang tham gia cuộc thi, không thể tạo đội mới. Vui lòng chuyển quyền hoặc giải tán team trước khi tạo đội mới.");
                    }

                    if (teamStatus == TeamStatus.ACTIVE) {
                        throw new BadRequestException("Bạn không thể tạo Team mới do bạn đang tham gia một đội khác. "
                                + "Vui lòng rời đội cũ trước khi tạo team mới.");
                    }

                    if (teamStatus == TeamStatus.BUSY) {
                        throw new BadRequestException(
                                "Bạn không thể tạo đội mới do bạn đang tham gia một cuộc thi chưa kết thúc. "
                                        + "Vui lòng đợi khi cuộc thi kết thúc để tiến hành việc tạo đội.");
                    }
                    if (teamStatus == TeamStatus.DRAFT) {
                        throw new BadRequestException("Bạn không thể tạo đội mới do bạn đã tạo một đội  trước đó.");
                    }
                }
            }

        }
        List<TeamDraft> drafts = teamDraftRepository.findAllByStatus(TeamStatus.DRAFT);
        if (!drafts.isEmpty()) {
            throw new BadRequestException("Bạn không được tạo đội mới do bạn đã tạo một đội thi khác trước đó");
        }

        // 3. BR: bắt buộc mời ít nhất 1 người khác
        if (request.getMemberEmails() == null || request.getMemberEmails().isEmpty()) {
            throw new BadRequestException("Khi tạo đội, bạn bắt buộc phải mời ít nhất 1 thành viên khác tham gia!");
        }

        // 3.2 Check duplicate member and loc email
        Set<String> cleanEmails = new HashSet<>();
        for (String email : request.getMemberEmails()) {
            if (email != null && !email.isBlank()) {
                cleanEmails.add(email.trim());
            }
        }

        if (cleanEmails.isEmpty()) {
            throw new BadRequestException("Danh sách email mời vào nhóm không hợp lệ!");
        }

        // 3.3 Check Email nhập vào có hợp lệ không (nếu là Leader của nhóm mình thì ko
        // được )
        if (cleanEmails.contains(leaderAccount.getEmail().trim())) {
            throw new BadRequestException("Bạn là Trưởng nhóm, không cần tự mời chính mình!");
        }

        // 3.4 Check account được gửi mail nếu ko có role Là STUDENT thì ko được phép
        // nhập
        Map<String, Account> memberAccountMap = new HashMap<>();
        for (String memberEmail : cleanEmails) {
            Optional<Account> checkAcc = accRepository.findByEmail(memberEmail);
            if (checkAcc.isPresent()) {
                Account acc = checkAcc.get();
                if (acc.getRole() == AccountRole.EVENTCOORDINATOR || acc.getRole() == AccountRole.ADMIN
                        || acc.getRole() == AccountRole.EXPERT) {
                    throw new BadRequestException("Email " + memberEmail
                            + " không hợp lệ. Bạn chỉ có thể mời tài khoản có vai trò là STUDENT.");

                }

                // Check ng được mời có tham gian team khác ko (BUSY)
                if (acc.getStudent() != null) {
                    List<TeamMember> memberExistingTeams = teamMemberRepository.findByStudent(acc.getStudent());
                    for (TeamMember tm : memberExistingTeams) {
                        if (tm.getTeam().getStatus() == TeamStatus.BUSY) {
                            throw new BadRequestException(
                                    "Sinh viên có email " + memberEmail + " hiện đang bận tham gia một cuộc thi khác.");
                        }
                    }
                }
                memberAccountMap.put(memberEmail, acc);
            }
        }

        // 3.5 Check trùng tên Nhóm
        boolean existName = teamDraftRepository.existsByTeamNameIgnoreCase(request.getTeamName().trim());
        if (existName) {
            throw new BadRequestException("Tên nhóm này đã được đăng ký trong cuộc thi này rồi!");
        }
        String teamName = request.getTeamName().trim();
        TeamDraft teamDraft = new TeamDraft();
        teamDraft.setTeamName(teamName);
        teamDraft.setAccount(leaderAccount);
        teamDraft.setTeamSize(1);
        teamDraft.setStatus(TeamStatus.DRAFT);
        TeamDraft savedPendingTeam = teamDraftRepository.save(teamDraft);

        // 5. Tạo object, save info of leader vao ListMember
        List<TeamResponse.MemberInfo> listMember = new ArrayList<>();
        TeamResponse.MemberInfo leaderInfo = TeamResponse.MemberInfo.builder()
                .studentCode(leaderAccount.getStudent().getStudentCode())
                .fullName(leaderAccount.getStudent().getStudentName())
                .email(leaderAccount.getEmail())
                .major(leaderAccount.getStudent().getMajor())
                .build();
        listMember.add(leaderInfo);
        List<String> invitedEmails = new ArrayList<>();

        // 6. Tao loi moi gui toi cac thah vien
        for (String memberEmail : cleanEmails) {
            Account account = memberAccountMap.get(memberEmail);
            TeamInvitation invitation = new TeamInvitation();
            invitation.setTeam(null);
            invitation.setAccount(account);
            invitation.setTeamDraft(teamDraft);
            invitation.setType(InvitationType.INVITATION);
            invitation.setStatus(InvitationStatus.PENDING);
            invitation.setCreatedAt(LocalDateTime.now());
            invitation.setEmail(memberEmail);

            TeamInvitation savedInvitation = teamInvitationRepository.save(invitation);
            invitedEmails.add(memberEmail);

            // 7. Gui loi moi den cac thnah vien
            try {
                MailRequest mailRequest = new MailRequest();
                mailRequest.setTo(memberEmail);
                mailRequest.setSubject("FPT HACKATHON - Team Invitation: " + teamName);

                Map<String, Object> props = new HashMap<>();
                props.put("studentName",
                        (account.getStudent() != null) ? account.getStudent().getStudentName() : memberEmail);
                props.put("teamName", teamName);
                props.put("leaderName", leaderAccount.getStudent().getStudentName());
                props.put("email", leaderAccount.getEmail());
                props.put("receiverEmail", memberEmail);
                props.put("invitationId", savedInvitation.getTeamInvitationId());
                mailRequest.setProps(props);
                emailService.sendEmail(mailRequest, "invitation");

            } catch (Exception e) {
                System.out.println("Email error: " + e.getMessage());
            }
            if (memberEmail != null) {
                notificationService.notifyInviteTeam(leaderAccount, memberAccountMap.get(memberEmail),
                        "Bạn nhận được lời mời tham gia đội " + teamName, savedInvitation.getTeamInvitationId());
            }
            // TAO THEM MOT CAI THONG BAO QUA WEB KHI CO LOI MOI

        }
        auditService.saveLog(leaderAccount, AuditAction.CREATE_TEAM, AuditEntityType.TEAM, null,
                "Create team " + teamName);
        // 8. Return TeamResponse
        return new TeamResponse(null, teamName, leaderInfo, listMember, LocalDateTime.now(), invitedEmails);
    }

    @Transactional
    @Override
    // Gửi lời mời vào đội sau khi kiểm tra quyền trưởng nhóm và tình trạng người
    // được mời.
    public TeamResponse sendTeamInvitation(InviteTeamRequest request, CustomUserDetails userDetails) {
        // 1. Leader gửi lời mời đến thành viên mình mong muốn
        Account leaderAcc = userDetails.getAccount();
        if (leaderAcc == null || leaderAcc.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản leader không hợp lệ.");
        }

        // TH1 ch đủ thành viên tối thiểu
        TeamDraft teamDraft = teamDraftRepository.findByAccount(leaderAcc)
                .orElse(null);

        // TH2 đủ thành viên tối thiểu
        TeamMember officialTeam = null;
        // Nếu TeamDraft không còn là DRAFT thì coi như Team chính thức
        if (teamDraft == null || teamDraft.getStatus() != TeamStatus.DRAFT) {
            List<TeamMember> leaderTeams = teamMemberRepository.findByStudent(leaderAcc.getStudent());
            for (TeamMember tm : leaderTeams) {
                if (tm.getTeam().getStatus() != TeamStatus.FINISHED && Boolean.TRUE.equals(tm.getIsLeader())) {
                    officialTeam = tm;
                    break;
                }
            }

            if (officialTeam == null) {
                throw new BadRequestException(
                        "Bạn không có quyền mời thành viên (Bạn không phải Trưởng nhóm của đội nào đang hoạt động).");
            }
            Team team = officialTeam.getTeam();
            this.checkEventRegistrationWindow(team);
            // Không dùng TeamDraft nữa
            teamDraft = null;
        }

        String teamName = (officialTeam != null)
                ? officialTeam.getTeam().getTeamName()
                : teamDraft.getTeamName();

        // 3. Check duplicate member and loc email
        Set<String> cleanEmails = new HashSet<>();
        for (String email : request.getMemberEmails()) {
            if (email != null && !email.isBlank()) {
                cleanEmails.add(email.trim());
            }
        }
        if (cleanEmails.isEmpty()) {
            throw new BadRequestException("Danh sách email mời vào nhóm không hợp lệ!");
        }
        // 3.1 Check Email nhập vào có hợp lệ không (nếu là Leader của nhóm mình thì ko
        // được )
        if (cleanEmails.contains(leaderAcc.getEmail().trim())) {
            throw new BadRequestException("Bạn là Trưởng nhóm, không cần tự mời chính mình!");
        }

        // 6. Tao loi moi gui toi cac thah vien
        // Lấy danh sách EventIds mà Team này đã đăng ký để check trùng
        List<Integer> eventIds;
        if (officialTeam != null) {
            List<Registration> teamRegistrations = registrationRepository.findByTeam(officialTeam.getTeam());
            eventIds = teamRegistrations.stream().map(r -> r.getHackathonEvent().getEventId()).toList();
        } else {
            eventIds = new ArrayList<>();
        }

        List<String> successfulInvites = new ArrayList<>();
        for (String memberEmail : cleanEmails) {
            Optional<Account> checkAcc = accRepository.findByEmail(memberEmail.trim());
            Account memberAccount = checkAcc.orElse(null);

            if (memberAccount != null) {

                // Chặn gửi mail đến những ng ko có role là Studnet
                if (memberAccount.getRole() != AccountRole.STUDENT) {
                    throw new BadRequestException(
                            "Email " + memberEmail + " không thuộc vai trò STUDENT, bạn không được phép mời.");
                }
                // Nếu là team chính thức, check xem đã ở trong team chưa hoặc bận event khác
                // chưa
                if (officialTeam != null) {
                    boolean isAlreadyInTeam = teamMemberRepository
                            .findByTeamIdAndStudentId(officialTeam.getId(), memberAccount.getStudent().getStudentId())
                            .isPresent();
                    if (isAlreadyInTeam) {
                        throw new BadRequestException("Sinh viên này đã là thành viên trong đội của bạn rồi.");
                    }

                    boolean alreadyInvitedOfficial = teamInvitationRepository.existsByTeamAndAccountAndStatus(
                            officialTeam.getTeam(), memberAccount, InvitationStatus.PENDING);
                    if (alreadyInvitedOfficial) {
                        throw new BadRequestException(
                                "Bạn đã gửi lời mời cho sinh viên " + memberEmail + " và đang chờ phản hồi (PENDING).");
                    }

                    List<TeamMember> studentTeams = teamMemberRepository.findByStudent(memberAccount.getStudent());
                    for (TeamMember tm : studentTeams) {
                        if (tm.getTeam().getStatus() != TeamStatus.FINISHED) {
                            // Check trùng event
                            List<Registration> regs = registrationRepository.findByTeam(tm.getTeam());
                            if (regs.stream().anyMatch(r -> eventIds.contains(r.getHackathonEvent().getEventId()))) {
                                throw new BadRequestException("Sinh viên " + memberEmail
                                        + " đã tham gia một đội thi khác trong cùng sự kiện.");
                            }
                            if (tm.getTeam().getStatus() == TeamStatus.BUSY) {
                                throw new BadRequestException(
                                        "Sinh viên " + memberEmail + " hiện đang bận ở đội khác.");
                            }
                        }

                    }
                } else if (teamDraft != null) {
                    boolean alreadyInvited = teamInvitationRepository.existsByTeamDraftAndAccount(teamDraft,
                            memberAccount);
                    if (alreadyInvited) {
                        throw new BadRequestException("Bạn đã gửi lời mời cho sinh viên " + memberEmail + " rồi.");
                    }
                }
            } else {
                // NẾU TÀI KHOẢN CHƯA TỒN TẠI: Check xem email này đã từng được mời vào Draft
                // chưa để chống spam rác DB
                if (teamDraft != null) {
                    boolean emailAlreadyInvited = teamInvitationRepository.existsByTeamDraftAndEmail(teamDraft,
                            memberEmail);
                    if (emailAlreadyInvited) {
                        throw new BadRequestException("Bạn đã gửi lời mời đến email " + memberEmail + " rồi.");
                    }
                }
            }
            TeamInvitation invite = new TeamInvitation();
            invite.setAccount(memberAccount);
            if (officialTeam != null) {
                invite.setTeam(officialTeam.getTeam());
            } else {
                invite.setTeamDraft(teamDraft);
            }
            invite.setType(InvitationType.INVITATION);
            invite.setStatus(InvitationStatus.PENDING);
            invite.setCreatedAt(LocalDateTime.now());
            invite.setEmail(memberEmail);
            TeamInvitation savedNoti = teamInvitationRepository.save(invite);
            successfulInvites.add(memberEmail);

            // 7. Gui loi moi den cac thanh vien
            try {
                MailRequest mailRequest = new MailRequest();
                mailRequest.setTo(memberEmail);
                mailRequest.setSubject("FPT HACKATHON - Team Invitation: " + teamName);

                Map<String, Object> props = new HashMap<>();
                props.put("studentName",
                        (memberAccount != null && memberAccount.getStudent() != null)
                                ? memberAccount.getStudent().getStudentName()
                                : memberEmail);
                props.put("teamName", teamName);
                props.put("leaderName", leaderAcc.getStudent().getStudentName());
                props.put("email", leaderAcc.getEmail());
                props.put("receiverEmail", memberEmail);
                props.put("invitationId", savedNoti.getTeamInvitationId());
                mailRequest.setProps(props);
                emailService.sendEmail(mailRequest, "invitation");

            } catch (Exception e) {
                System.out.println("Email error: " + e.getMessage());
            }

            if (memberAccount != null) {
                notificationService.notifyInviteTeam(leaderAcc, memberAccount,
                        "Bạn nhận được lời mời tham gia đội " + teamName, savedNoti.getTeamInvitationId());
            } else {
                System.out.println("Không gọi thông báo web vì Email chưa có tài khoản");
            }
        }
        Integer resTeamId = (officialTeam != null) ? officialTeam.getTeam().getTeamId() : null;
        // 8. Return TeamResponse
        return TeamResponse.builder().teamId(resTeamId)
                .teamName(teamName)
                .createAt(teamDraft != null ? teamDraft.getCreateAt() : officialTeam.getTeam().getCreateAt())
                .invitedEmails(successfulInvites).build();

    }

    // FUNCTION 2:UPDATE INFORMATION ABOUT TEAM AS NAME
    @Override
    @Transactional
    // Cập nhật tên đội sau khi xác nhận người thao tác là trưởng nhóm hợp lệ.
    public String updateInfo(CustomUserDetails userDetails, String teamName) {

        // 1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        Account currentAccount = userDetails.getAccount();

        // 2. Check account nay co tham gia Team này không
        List<TeamMember> currentMember = teamMemberRepository.findByStudent(currentAccount.getStudent());
        if (currentMember.isEmpty()) {
            throw new BadRequestException("Bạn hiện chưa tham gia bất kỳ đội nào trong hệ thống!");
        }
        TeamMember leaderRole = currentMember.stream().filter(tm -> tm.getTeam().getStatus() != TeamStatus.FINISHED) // Chỉ
                                                                                                                     // xét
                                                                                                                     // nhóm
                                                                                                                     // đang
                                                                                                                     // DRAFT
                                                                                                                     // hoặc
                                                                                                                     // BUSY
                .filter(TeamMember::getIsLeader).findFirst().orElseThrow(() -> new BadRequestException(
                        "Bạn chỉ là thành viên, không phải là trưởng nhóm. Bạn không có quyền thay đổi thông tin đội!"));

        // Lấy ra đối tượng Team từ dòng Leader tìm được
        Team team = leaderRole.getTeam();
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new BadRequestException("Không được phép chỉnh sửa thông tin đội thi khi đang tham giải đấu.");
        }
        // 4. Check Team này đã được phê duyệt đội khi gửi đơn đăng ký chưa
        List<Registration> registration = registrationRepository.findByTeam(team);
        if (registration != null && !registration.isEmpty()) {
            boolean hasActiveRegister = registration.stream()
                    .anyMatch(regis -> regis.getStatus().equals(RegistrationStatus.APPROVED)
                            || regis.getStatus().equals(RegistrationStatus.PENDING));
            if (hasActiveRegister) {
                throw new BadRequestException("Bạn không được phép thay đổi tên nhóm khi đã gửi đơn đăng ký Team.");
            }
        }

        // 5.Check deadline
        checkEventRegistrationWindow(team);

        // 5.1 Validate team name
        if (teamName == null || teamName.isBlank()) {
            throw new BadRequestException("Tên đội mới không được để trống!");
        }
        String cleanName = teamName.trim();
        if (cleanName.equalsIgnoreCase(team.getTeamName().trim())) {
            return team.getTeamName();
        }
        boolean existName = teamRepository.existsByTeamNameIgnoreCaseAndTeamIdNot(cleanName, team.getTeamId());
        boolean exxistDraftName = teamDraftRepository.existsByTeamNameIgnoreCase(cleanName);
        if (existName || exxistDraftName) {
            throw new BadRequestException(
                    "Tên nhóm '" + cleanName + "' đã được đăng ký bởi một đội khác trong hệ thống rồi!");
        }

        // 6. Update
        team.setTeamName(cleanName);
        teamRepository.save(team);
        auditService.saveLog(currentAccount, AuditAction.UPDATE_EVENT, AuditEntityType.EVENT, team.getTeamId(),
                "Update team " + team.getTeamName());
        return team.getTeamName();

    }

    @Override
    // Chấp nhận lời mời chính thức và thêm sinh viên vào đội sau khi kiểm tra điều
    // kiện.
    public void acceptOfficialInvite(TeamInvitation invitation, CustomUserDetails userDetails) {
        Team team = teamRepository.findById(invitation.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại hoặc đã bị xóa."));

        Notification notification = notificationRepository.findByTeamInvitation(invitation)
                .orElse(null);
        Account inviteAccount = userDetails.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }

        // 1. Kiểm tra hạn của lời mời
        int expireDays = systemConfigService.getIntConfig(SystemConfigKey.INVITATION_EXPIRE_DAYS);
        LocalDateTime expiredAt = invitation.getCreatedAt().plusDays(expireDays);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new BadRequestException("Lời mời tham gia của bạn đã hết hạn.");
        }

        // 2. Kiểm tra thời gian mở đăng ký sự kiện của team
        this.checkEventRegistrationWindow(team);

        // 3. Kiểm tra trùng sự kiện / bận của sinh viên (so với team này)
        this.validateStudentAvailability(inviteAccount.getStudent(), team);

        // 4. Kiểm tra xem user đã là thành viên của nhóm chưa
        boolean exists = teamMemberRepository.existsByTeamAndStudent(team, inviteAccount.getStudent());
        if (exists) {
            throw new BadRequestException("Bạn đã là thành viên của Team này từ trước rồi.");
        }

        // 5. Cập nhật trạng thái lời mời thành ACCEPTED
        invitation.setStatus(InvitationStatus.ACCEPTED);
        teamInvitationRepository.save(invitation);

        // 6. Thêm vào bảng TeamMember chính thức
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setIsLeader(false);
        member.setStudent(inviteAccount.getStudent());
        teamMemberRepository.save(member);

        // 7. Cập nhật tăng số lượng thành viên của team
        team.setTeamSize(Optional.ofNullable(team.getTeamSize()).orElse(0) + 1);
        teamRepository.save(team);

        // 8. Nếu đội đạt số lượng tối đa (maxTeam), tự động vô hiệu hóa các lời mời
        // PENDING còn lại
        int maxTeam = systemConfigService.getIntConfig(SystemConfigKey.MAX_TEAM_SIZE);
        if (team.getTeamSize() >= maxTeam) {
            List<TeamInvitation> otherInvites = teamInvitationRepository.findByTeam(team);
            for (TeamInvitation oldInvite : otherInvites) {
                if (!oldInvite.getTeamInvitationId().equals(invitation.getTeamInvitationId())
                        && oldInvite.getStatus() == InvitationStatus.PENDING) {
                    oldInvite.setStatus(InvitationStatus.INVALID);
                    teamInvitationRepository.save(oldInvite);
                }
            }
        }
        if (notification != null) {
            String teamName = team.getTeamName();
            notification.setTitle("INVITATION ACCEPTED. Bạn đã tham gia Team: " + teamName);
            notification.setMessage("Bạn đã trở thành thành viên  của " + teamName);
            notification.setRead(true); // Đánh dấu đã đọc
            notification.setResponseStatus(NotiResponseStatus.NONE);
            notification.setResponseAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    // FUNCTION 3: RỜI TEAM

    @Transactional
    @Override
    // Cho thành viên rời đội nếu không vi phạm ràng buộc đăng ký hoặc vai trò
    // trưởng nhóm.
    public void leaveTeam(CustomUserDetails userDetails, Integer teamId) {
        Account currentUser = userDetails.getAccount();
        Student student = currentUser.getStudent();

        // 1. CỐ GẮNG TÌM XEM ĐÂY CÓ PHẢI TEAM CHÍNH THỨC KHÔNG
        Optional<TeamMember> teamMemberOpt = (student != null)
                ? teamMemberRepository.findByTeam_TeamIdAndStudent(teamId, student)
                : Optional.empty();

        if (teamMemberOpt.isPresent()) {
            // --- XỬ LÝ KHI LÀ TEAM CHÍNH THỨC ---
            TeamMember teamMember = teamMemberOpt.get();
            Team team = teamMember.getTeam();

            checkEventRegistrationWindow(team);

            int countMember = team.getTeamSize();
            if (teamMember.getIsLeader() && countMember > 1) {
                throw new BadRequestException("Leader phải chuyển quyền cho thành viên khác trước khi rời team.");
            }

            // TH1: Đội chính thức chỉ còn đúng 1 người -> Xóa team luôn
            if (countMember == 1) {
                team.setStatus(TeamStatus.DELETED);
                team.setTeamSize(0);
                teamRepository.save(team);
                teamMemberRepository.delete(teamMember);

                teamDraftRepository.findByAccount_AccountIdAndStatus(currentUser.getAccountId(), TeamStatus.DRAFT)
                        .ifPresent(draft -> {
                            draft.setStatus(TeamStatus.DELETED);
                            teamDraftRepository.save(draft);
                        });

                auditService.saveLog(currentUser, AuditAction.UPDATE_TEAM, AuditEntityType.TEAM, team.getTeamId(),
                        "Xóa team chính thức: " + team.getTeamName());
                return;
            }

            // TH2: Team chính thức có nhiều thành viên -> Rời nhóm bình thường
            teamMemberRepository.delete(teamMember);
            team.setTeamSize(Math.max(0, team.getTeamSize() - 1));
            teamRepository.save(team);

            auditService.saveLog(currentUser, AuditAction.UPDATE_TEAM, AuditEntityType.TEAM, team.getTeamId(),
                    "Rời team chính thức: " + team.getTeamName());
            return;
        }

        // 2. NẾU KHÔNG THẤY TRONG TEAM CHÍNH THỨC -> TỰ ĐỘNG HIỂU ĐÂY LÀ TEAM DRAFT
        // Lúc này tham số teamId truyền vào thực chất chính là teamDraftId
        TeamDraft draft = teamDraftRepository.findById(Long.valueOf(teamId))
                .orElseThrow(() -> new BadRequestException("Không tìm thấy team hoặc draft với ID này."));

        boolean isDraftOwner = draft.getAccount() != null
                && draft.getAccount().getAccountId() == (currentUser.getAccountId());

        if (isDraftOwner) {
            // Chủ nhóm Draft bấm rời -> Xóa/hủy luôn Draft
            draft.setStatus(TeamStatus.DELETED);
            teamDraftRepository.save(draft);

            auditService.saveLog(
                    currentUser,
                    AuditAction.UPDATE_TEAM,
                    AuditEntityType.TEAM,
                    Math.toIntExact(draft.getTeamDraftId()),
                    "Xóa team draft: " + draft.getTeamName());
        } else {
            // Thành viên khác rời Team Draft
            TeamInvitation invitation = teamInvitationRepository
                    .findByTeamDraftAndAccount(draft, currentUser)
                    .orElseThrow(() -> new BadRequestException("Bạn không thuộc Team Draft này."));

            teamInvitationRepository.delete(invitation);

            draft.setTeamSize(Math.max(0, draft.getTeamSize() - 1));
            teamDraftRepository.save(draft);

            auditService.saveLog(
                    currentUser,
                    AuditAction.UPDATE_TEAM,
                    AuditEntityType.TEAM,
                    Math.toIntExact(draft.getTeamDraftId()),
                    "Rời team draft: " + draft.getTeamName());
        }
    }

    // FUNCTION 4: CHUYỂN QUYỀN LEADER(Chỉ mới gửi lời mời đến thành viên muốn
    // chuyển quyền )
    @Override
    @Transactional
    // Gửi yêu cầu chuyển quyền trưởng nhóm cho một thành viên đủ điều kiện.
    public void transferLeader(Integer teamId, TeamRequestDTO request, CustomUserDetails userDetails) {

        // 2. Check Team
        Account currentUser = userDetails.getAccount();
        TeamMember teamMember = teamMemberRepository.findByTeam_TeamIdAndStudent(teamId, currentUser.getStudent())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn hiện không tham gia hoặc không phải thành viên của đội này!"));
        Team team = teamMember.getTeam();

        // 3. Check Team đã được phê duyệt chưa(xem lại bussiness rule)
        // 4. Check Leader có thuộc Team ko
        if (!teamMember.getIsLeader()) {
            throw new BadRequestException("Chỉ Leader hiện tại mới được quyền chuyển quyền Trưởng nhóm.");
        }

        // 5.CheckDeadline
        this.checkEventRegistrationWindow(team);

        // 5. Check Student được chuyển quyền có thuộc Team ko
        Student newLeader = studentRepository.findByStudentCode(request.getStudentCode());
        if (newLeader == null || newLeader.getAccount() == null) {
            throw new BadRequestException("Không tìm thấy sinh viên được chọn.");
        }
        // 6. Check Student được chuyển quyền có thuộc team này không
        boolean isStudent = teamMemberRepository.existsByTeamAndStudent(team, newLeader);
        if (!isStudent) {
            throw new BadRequestException("Sinh viên không thuộc Team này.");
        }

        // 6. Check leader không tự chuyển quyền cho mình
        if (currentUser.getStudent().getStudentCode().equalsIgnoreCase(newLeader.getStudentCode())) {
            throw new BadRequestException("Bạn đang là Leader của team. Bạn không thể tự chuyển quyền cho chính mình.");
        }

        // 8. Tạo thông báo gửi lời mời
        TeamInvitation inviteTransfer = new TeamInvitation();

        inviteTransfer.setAccount(newLeader.getAccount());
        inviteTransfer.setTeam(team);
        inviteTransfer.setType(InvitationType.LEADER_TRANSFER_REQUEST);
        inviteTransfer.setStatus(InvitationStatus.PENDING);
        inviteTransfer.setCreatedAt(LocalDateTime.now());
        TeamInvitation savedNoti = teamInvitationRepository.save(inviteTransfer);

        // 9. Gửi lời mời
        try {
            MailRequest mailRequest = new MailRequest();
            mailRequest.setTo(newLeader.getAccount().getEmail());
            mailRequest.setSubject("FPT HACKATHON. Lời mời chuyển quyền Leader cho cuộc thi Hackathon" + " đến từ Team "
                    + team.getTeamName());
            Map<String, Object> props = new HashMap<>();
            props.put("studentName", newLeader.getStudentName());
            props.put("teamName", team.getTeamName());
            props.put("leaderName", currentUser.getStudent().getStudentName());
            props.put("email", currentUser.getEmail());
            props.put("receiverEmail", newLeader.getAccount().getEmail());
            props.put("invitationId", savedNoti.getTeamInvitationId());
            mailRequest.setProps(props);
            emailService.sendEmail(mailRequest, "transfer");
        } catch (Exception e) {
            System.out.println(" Lỗi gửi email chuyển quyền leader: " + e.getMessage());
        }
        auditService.saveLog(currentUser, AuditAction.UPDATE_TEAM, AuditEntityType.TEAM, team.getTeamId(),
                "Transfer leader " + team.getTeamName() + "leader mới: " + newLeader);
    }

    // -------------------------------------//
    // XỬ LÝ LỜI MỜI: CHẤP NHẬN - TỪ CHỐI
    // -------------------------------------//

    // FUNCTION 5: HÀM XỬ LÝ CHẤP NHẬN LỜI MỜI CHO TRANSFER, INVITE TEAM
    @Override
    @Transactional
    public void acceptGeneralInvite(Long invitationId, CustomUserDetails userDetails) {
        // 2. Check account được nhận lời mời vs account được gửi lời mời có giống nhau
        // không(nhớ mở ra)
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại hoặc đã bị hủy từ trước."));
        if (userDetails != null && userDetails.getAccount() != null) {
            if (invitation.getAccount() != null) {
                if (invitation.getAccount().getAccountId() != userDetails.getAccount().getAccountId()) {
                    throw new BadRequestException("Bạn không có quyền truy cập vào thông báo này.");
                }
            }
        }

        // 3 . Check trạng thái của lời mời(hết hạn, chấp nhận, từ chối) rồi sẽ vô hiệu
        // hóa
        if (invitation.getStatus() == InvitationStatus.ACCEPTED ||
                invitation.getStatus() == InvitationStatus.REJECTED ||
                invitation.getStatus() == InvitationStatus.EXPIRED ||
                invitation.getStatus() == InvitationStatus.INVALID) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc không còn hiệu lực.");
        }
        // 4.1 Check trường hợp Team ko tồn tại
        Team team = invitation.getTeam();
        TeamDraft teamDraft = invitation.getTeamDraft();
        if (team == null && teamDraft == null) {
            invitation.setStatus(InvitationStatus.INVALID);
            teamInvitationRepository.save(invitation);
            throw new BadRequestException(
                    "Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
        }

        // 5. Nếu là team chính thức thì check thời gian mở đăng ký sự kiện
        this.checkEventRegistrationWindow(team);
        if (team != null) {
            if (team.getTeamSize() == null || team.getTeamSize() <= 0) {
                invitation.setStatus(InvitationStatus.INVALID);
                teamInvitationRepository.save(invitation);
                throw new BadRequestException(
                        "Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
            }
            this.checkEventRegistrationWindow(team);
        }
        // 6.
        switch (invitation.getType()) {
            case INVITATION:
                if (teamDraft != null) {
                    this.acceptTeamDraftInvite(invitation, userDetails);
                } else if (team != null) {
                    this.acceptOfficialInvite(invitation, userDetails);
                }
                break;

            case LEADER_TRANSFER_REQUEST:
                this.acceptLeaderTransfer(invitation, userDetails);
                break;

            default:
                throw new BadRequestException("Loại thông báo không hợp lệ.");
        }

    }

    @Override
    @Transactional // Khong roll Back khi dinh loi xu ly tb hong
    public void acceptTeamDraftInvite(TeamInvitation invitation, CustomUserDetails userDetails) {
        // 1. Tim thong bao or loi moi tuong ung
        // 2. Kiem tra team gui loi moi con ton tai khong
        // 3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Account inviteAccount = userDetails.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Bạn cần đăng nhập vào hệ thống để chấp nhận lời mời");

        }
        Notification notification = notificationRepository.findByTeamInvitation(invitation)
                .orElse(null);
        // Nếu invitation đã có account thì kiểm tra quyền
        if (invitation.getAccount() != null) {
            if (invitation.getEmail() == null ||
                    !invitation.getEmail().equalsIgnoreCase(inviteAccount.getEmail())) {
                throw new BadRequestException("Email đăng nhập không đúng với email được mời.");
            }

        } else {
            // Invitation trước đó chưa có account -> gán account hiện tại
            invitation.setAccount(inviteAccount);
            teamInvitationRepository.save(invitation);
        }

        TeamDraft teamDraft = invitation.getTeamDraft();

        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la N ngay, ke tu ngay gui thong bao
        int expireDays = systemConfigService.getIntConfig(SystemConfigKey.INVITATION_EXPIRE_DAYS);
        LocalDateTime expiredAt = invitation.getCreatedAt().plusDays(expireDays);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            // Neu loi moi het han , thi vo hieu hoa loi moi(cap nhat trang thai thong bao)
            invitation.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }

        // 4. Kiểm tra xem sinh viên này đã tham gia đội chính thức nào khác chưa hoặc
        // đang bận
        this.validateStudentAvailability(inviteAccount.getStudent(), null);

        // CHẶN: Kiểm tra xem sinh viên này đã ACCEPTED ở một TeamDraft khác chưa
        boolean hasAcceptedOtherDraft = teamInvitationRepository.existsByAccountAndStatusAndTeamDraftNot(
                inviteAccount,
                InvitationStatus.ACCEPTED,
                teamDraft);
        if (hasAcceptedOtherDraft) {
            throw new BadRequestException("Bạn đã chấp nhận tham gia một đội khác rồi, không thể tham gia đội này!");
        }

        // 5. Cap nhat thong boa khi ban Chap nhan loi moi

        invitation.setStatus(InvitationStatus.ACCEPTED);
        if (invitation.getAccount() == null) {
            invitation.setAccount(inviteAccount);
        }

        teamInvitationRepository.save(invitation);

        long acceptedCount = teamInvitationRepository.countByTeamDraftAndStatus(teamDraft, InvitationStatus.ACCEPTED);
        int currentTeamSize = (int) acceptedCount + 1;
        teamDraft.setTeamSize(currentTeamSize);
        teamDraftRepository.save(teamDraft);

        // 6. Check so luong thanh vien hien tai cua nhom
        int maxTeam = systemConfigService.getIntConfig(SystemConfigKey.MAX_TEAM_SIZE);
        int minTeam = systemConfigService.getIntConfig(SystemConfigKey.MIN_TEAM_SIZE);

        Team saveTeam = null;
        if (currentTeamSize >= minTeam) {
            Optional<Team> existingTeam = teamRepository.findByTeamNameIgnoreCase(teamDraft.getTeamName().trim());

            if (existingTeam.isPresent()) {
                saveTeam = existingTeam.get();
            } else {
                Team newTeam = new Team();
                newTeam.setTeamName(teamDraft.getTeamName().trim());
                newTeam.setStatus(TeamStatus.ACTIVE);
                newTeam.setTeamSize(currentTeamSize);
                saveTeam = teamRepository.save(newTeam);

                // Lưu thông tin Leader
                TeamMember leaderMember = new TeamMember();
                leaderMember.setTeam(saveTeam);
                leaderMember.setIsLeader(true);
                leaderMember.setStudent(teamDraft.getAccount().getStudent());
                teamMemberRepository.save(leaderMember);
            }
            // Lấy tất cả các thành viên đã ACCEPTED trong TeamDraft đưa sang TeamMember
            // chính thức
            List<TeamInvitation> acceptedInvitations = teamInvitationRepository.findByTeamDraftAndStatus(teamDraft,
                    InvitationStatus.ACCEPTED);
            for (TeamInvitation accInvite : acceptedInvitations) {
                if (accInvite.getAccount() != null && accInvite.getAccount().getStudent() != null) {
                    // Tránh thêm trùng leader nếu đã add ở trên
                    boolean isLeader = teamDraft.getAccount().equals(accInvite.getAccount());
                    if (!isLeader) {
                        TeamMember member = new TeamMember();
                        member.setTeam(saveTeam);
                        member.setIsLeader(false);
                        member.setStudent(accInvite.getAccount().getStudent());
                        teamMemberRepository.save(member);
                    }
                }
            }
        }

        // Xóa TeamDraft vì đã chính thức lên Team

        teamDraft.setStatus(TeamStatus.OFFICIAL);
        teamDraftRepository.save(teamDraft);

        if (currentTeamSize >= maxTeam) {
            List<TeamInvitation> otherInvites = teamInvitationRepository.findByTeamDraft(teamDraft);
            for (TeamInvitation oldInvite : otherInvites) {
                if (!oldInvite.getTeamInvitationId().equals(invitation.getTeamInvitationId())
                        && oldInvite.getStatus() == InvitationStatus.PENDING) {
                    oldInvite.setStatus(InvitationStatus.INVALID);
                    teamInvitationRepository.save(oldInvite);
                }
            }
        }
        if (notification != null) {
            String teamName = (saveTeam != null) ? saveTeam.getTeamName() : teamDraft.getTeamName();
            notification.setTitle("INVITATION ACCEPTED. Bạn đã tham gia Team: " + teamName);
            notification.setMessage("Bạn đã trở thành thành viên  của " + teamName);
            notification.setResponseStatus(NotiResponseStatus.NONE);
            notification.setResponseAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void acceptLeaderTransfer(TeamInvitation invitation, CustomUserDetails userDetails) {
        // 1. Tim tb hoac loi moi tuong ung
        // 2. Kiem tra Team loi moi con ton tai khong
        Team team = teamRepository.findById(invitation.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        // 3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Notification notification = notificationRepository.findByTeamInvitation(invitation)
                .orElse(null);
        Account inviteAccount = invitation.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }
        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        int expireDays = systemConfigService.getIntConfig(SystemConfigKey.INVITATION_EXPIRE_DAYS);
        LocalDateTime expiredAt = invitation.getCreatedAt().plusDays(expireDays);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }
        Student newLeaderStudent = inviteAccount.getStudent();

        // 5. Check người leader gửi lời mời vs ng làm leader hiện tại có trùng khớp ko
        // Tránh trường hợp gửi lời mời cho 2 người và ng kia đồng ý trước
        TeamMember currentLeader = teamMemberRepository.findByTeamAndIsLeader(team, true)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy leader hiện tại"));

        if (currentLeader.getStudent().getStudentCode().equalsIgnoreCase(newLeaderStudent.getStudentCode())) {
            throw new BadRequestException("Bạn đã là Trưởng nhóm của đội này rồi.");
        }

        // 6. Update leader mới
        currentLeader.setIsLeader(false);

        TeamMember newLeader = teamMemberRepository.findByTeamAndStudent(team, newLeaderStudent)
                .orElseThrow(() -> new BadRequestException(
                        "Bạn hiện không phải là thành viên của đội này nên không thể nhận quyền Trưởng nhóm."));

        newLeader.setIsLeader(true);
        teamMemberRepository.save(currentLeader);
        teamMemberRepository.save(newLeader);
        //
        invitation.setStatus(InvitationStatus.ACCEPTED);
        teamInvitationRepository.save(invitation);
        if (notification != null) {
            notification.setTitle("TRANSFER APPROVED. Bạn đã là Leader của Team: " + team.getTeamName());
            notification.setMessage("Bạn đã chấp nhận lời mời và chính thức trở thành Trưởng nhóm.");
            notification.setStatus(InvitationStatus.ACCEPTED);
            notification.setResponseAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

    }

    // TỪ CHỐI LỜI MỜI
    @Override
    @Transactional
    public void rejectGeneralInvite(Long invitationId, CustomUserDetails userDetails) {

        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại hoặc đã bị hủy từ trước."));
        if (userDetails != null && userDetails.getAccount() != null) {
            if (invitation.getAccount() != null) {
                if (invitation.getAccount().getAccountId() != userDetails.getAccount().getAccountId()) {
                    throw new BadRequestException("Bạn không có quyền truy cập vào thông báo này.");
                }
            }
        }

        // 4.1 Check trường hợp Team ko tồn tại
        Team team = invitation.getTeam();
        TeamDraft teamDraft = invitation.getTeamDraft();
        if (team == null && teamDraft == null) {
            invitation.setStatus(InvitationStatus.INVALID);
            teamInvitationRepository.save(invitation);
            throw new BadRequestException(
                    "Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
        }

        // 5. Nếu là team chính thức thì check thời gian mở đăng ký sự kiện
        this.checkEventRegistrationWindow(team);
        if (team != null) {
            if (team.getTeamSize() == null || team.getTeamSize() <= 0) {
                invitation.setStatus(InvitationStatus.INVALID);
                teamInvitationRepository.save(invitation);
                throw new BadRequestException(
                        "Đội hình này hiện không còn thành viên nào hoạt động, lời mời đã bị vô hiệu hóa.");
            }
            this.checkEventRegistrationWindow(team);
        }

        // 2. Check account được nhận lời mời vs account được gửi lời mời có giống nhau
        // không
        if (userDetails != null && userDetails.getAccount() != null) {
            if (invitation.getAccount().getAccountId() != userDetails.getAccount().getAccountId()) {
                throw new BadRequestException("Bạn không có quyền truy cập vào thông báo này.");
            }
        }

        // 4. Check trạng thái của lời mời(hết hạn, chấp nhận, từ chối) rồi sẽ vô hiệu
        // hóa
        if (invitation.getStatus() == InvitationStatus.ACCEPTED || invitation.getStatus() == InvitationStatus.REJECTED
                || invitation.getStatus() == InvitationStatus.EXPIRED
                || invitation.getStatus() == InvitationStatus.INVALID) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc không còn hiệu lực.");
        }
        // // 5. Check thời hạn(ĐK: CÙNG BUSINESS RULE)
        // checkEventRegistrationWindow(team);

        switch (invitation.getType()) {
            case INVITATION:
                this.rejectTeamInvite(invitation, userDetails);
                break;

            case LEADER_TRANSFER_REQUEST:
                this.rejectLeaderTransferInvite(invitation, userDetails);
                break;
            default:
                throw new BadRequestException("Loại thông báo không hợp lệ để thực hiện thao tác từ chối.");
        }
        teamInvitationRepository.save(invitation);
    }

    @Override
    public void rejectLeaderTransferInvite(TeamInvitation invitation, CustomUserDetails userDetails) {
        // 1. Tim tb hoac loi moi tuong ung
        // 2. Kiem tra Team loi moi con ton tai khong
        Team team = teamRepository.findById(invitation.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại"));

        Notification notification = notificationRepository.findByTeamInvitation(invitation)
                .orElse(null);
        // 3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification
        Account inviteAccount = invitation.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }
        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        int expireDays = systemConfigService.getIntConfig(SystemConfigKey.INVITATION_EXPIRE_DAYS);
        LocalDateTime expiredAt = invitation.getCreatedAt().plusDays(expireDays);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        if (notification != null) {
            notification.setTitle("TRANSFER REJECTED. Tôi từ chối làm Leader Team: " + team.getTeamName());
            notification.setMessage("Bạn đã từ chối lời mời chuyển quyền Leader.");
            notification.setStatus(InvitationStatus.REJECTED);
        }

    }

    @Override
    public void rejectTeamInvite(TeamInvitation invitation, CustomUserDetails userDetails) {
        // 1. Tim tb hoac loi moi tuong ung
        // 2. Kiem tra Team loi moi con ton tai khong
        Team team = teamRepository.findById(invitation.getTeam().getTeamId())
                .orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        // 3.Lấy tài khoản nhận thông báo trực tiếp từ bản ghi Notification

        Notification notification = notificationRepository.findByTeamInvitation(invitation)
                .orElse(null);

        Account inviteAccount = invitation.getAccount();
        if (inviteAccount == null || inviteAccount.getStudent() == null) {
            throw new BadRequestException("Thông tin tài khoản nhận lời mời không hợp lệ.");
        }

        // 4.Check hạn của lời mời
        // Thoi han cua loi moi nay la 3 ngay, ke tu ngay gui thong bao(ngày tạo)
        int expireDays = systemConfigService.getIntConfig(SystemConfigKey.INVITATION_EXPIRE_DAYS);
        LocalDateTime expiredAt = invitation.getCreatedAt().plusDays(expireDays);
        if (LocalDateTime.now().isAfter(expiredAt)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new BadRequestException("Lời mời tham gia của bạn hết hạn");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        if (notification != null) {
            notification.setTitle("INVITE REJECTED. Tôi từ chối lời mời tham gia nhóm : " + team.getTeamName());
            notification.setMessage("Bạn đã từ chối lời mời tham gia nhóm.");
            notification.setStatus(InvitationStatus.REJECTED);
        }

    }

    // -------------------------------------//
    // XEM THÔNG TIN VỀ TEAM
    // -------------------------------------//

    @Override
    @Transactional()
    // Lấy đội hiện tại và thông tin thành viên của sinh viên đang đăng nhập.
    public TeamDetailResponse getTeamDetailByStudentId(CustomUserDetails userDetails) {
        Account currentAccount = userDetails.getAccount();
        TeamDraft teamDraft = teamDraftRepository.findByAccount(currentAccount).orElse(null);
        List<TeamMember> members = teamMemberRepository
                .findByStudent_StudentId(currentAccount.getStudent().getStudentId());

        // 1. Kiểm tra xem user có team chính thức hoặc team draft nào không
        if (teamDraft == null && members.isEmpty()) {
            throw new BadRequestException("Bạn hiện chưa tham gia hoặc tạo đội nào.");
        }

        // Trường hợp đã có team chính thức
        if (!members.isEmpty()) {
            Team team = teamRepository.findById(members.get(0).getTeam().getTeamId())
                    .orElseThrow(() -> new BadRequestException("Team không tồn tại"));

            // 2. Check account đang đăng nhập có đang là thành viên của Team đó hay không
            TeamMember teamMember = teamMemberRepository.findByTeamAndStudent(team, currentAccount.getStudent())
                    .orElseThrow(() -> new BadRequestException(
                            "Sinh viên hiện tại không thuộc Team này. Không được phép xem danh sách Team này."));

            // 3. Lấy danh sách teamMember
            List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
            TeamDetailResponse.MemberInfo leaderInfo = null;
            List<TeamDetailResponse.MemberInfo> officialMembers = new ArrayList<>();
            for (TeamMember member : teamMembers) {
                TeamDetailResponse.MemberInfo info = TeamDetailResponse.MemberInfo.builder()
                        .studentCode(member.getStudent().getStudentCode())
                        .fullName(member.getStudent().getStudentName())
                        .email(member.getStudent().getAccount().getEmail())
                        .avatarUrl(member.getStudent().getAccount().getAvatarUrl())
                        .isLeader(member.getIsLeader())
                        .major(member.getStudent().getMajor())
                        .build();

                if (member.getIsLeader()) {
                    leaderInfo = info;
                } else {
                    officialMembers.add(info);
                }
            }
            // 4. Lấy danh sách các email đã gửi lời mời
            List<TeamDetailResponse.InviteInfo> inviteInfo = new ArrayList<>();
            // Chỉ khi người đang xem là LEADER thì mới xem được lời mời
            if (teamMember.getIsLeader()) {
                List<TeamInvitation> invites;
                Optional<TeamDraft> teamDraftOpt = teamDraftRepository.findByTeamNameIgnoreCase(team.getTeamName());
                if (teamDraftOpt.isPresent()) {
                    // Team đang draft
                    invites = teamInvitationRepository.findByTeamDraft(teamDraftOpt.get());
                } else {
                    // Team đã chính thức
                    invites = teamInvitationRepository.findByTeam(team);
                }
                for (TeamInvitation invite : invites) {
                    inviteInfo.add(new TeamDetailResponse.InviteInfo(
                            invite.getEmail(),
                            invite.getStatus().name()));
                }
            }

            return TeamDetailResponse.builder()
                    .teamId(team.getTeamId())
                    .teamName(team.getTeamName())
                    .leader(leaderInfo)
                    .members(officialMembers)
                    .sizeTeam(teamMembers.size())
                    .invitations(inviteInfo)
                    .build();
        }
        // Trường hợp 2 teamDraft (chưa có team chính thức)
        TeamDetailResponse.MemberInfo leaderInfo = TeamDetailResponse.MemberInfo.builder()
                .studentCode(currentAccount.getStudent().getStudentCode())
                .fullName(currentAccount.getStudent().getStudentName())
                .email(currentAccount.getEmail())
                .avatarUrl(currentAccount.getAvatarUrl())
                .isLeader(true)
                .major(currentAccount.getStudent().getMajor())
                .build();
        // Lấy danh sách lời mời đã gửi của đội nháp
        List<TeamInvitation> invites = teamInvitationRepository.findByTeamDraft(teamDraft);
        List<TeamDetailResponse.InviteInfo> inviteInfo = new ArrayList<>();
        List<TeamDetailResponse.MemberInfo> draftMembers = new ArrayList<>();
        for (TeamInvitation invite : invites) {

            if (invite.getStatus().name().equals("ACCEPTED")) {
                // Tìm thông tin sinh viên dựa theo email hoặc tài khoản của lời mời
                Optional<Student> studentOpt = studentRepository.findByAccount_Email(invite.getEmail());
                if (studentOpt.isPresent()) {
                    Student student = studentOpt.get();
                    draftMembers.add(TeamDetailResponse.MemberInfo.builder()
                            .studentCode(student.getStudentCode())
                            .fullName(student.getStudentName())
                            .email(invite.getEmail())
                            .avatarUrl(student.getAccount() != null ? student.getAccount().getAvatarUrl() : null)
                            .isLeader(false)
                            .major(student.getMajor())
                            .build());
                }
            } else {
                // Các trạng thái PENDING,... đưa vào danh sách lời mời đang chờ
                inviteInfo.add(new TeamDetailResponse.InviteInfo(
                        invite.getEmail(),
                        invite.getStatus().name()));
            }
        }
        int totalTeamSize = 1 + draftMembers.size();
        return TeamDetailResponse.builder()
                .teamId(teamDraft.getTeamDraftId().intValue())
                .teamName(teamDraft.getTeamName())
                .leader(leaderInfo)
                .members(new ArrayList<>()) // Đội nháp chưa có thành viên chính thức nào khác ngoài leader
                .sizeTeam(totalTeamSize)
                .invitations(inviteInfo)
                .build();
    }

    // FUNCTION STUDENT XEM THÔNG TIN TEAM CỦA MÌNH
    @Override
    // Lấy danh sách thành viên của đội sau khi kiểm tra quyền xem thông tin.
    public TeamDetailResponse getTeamMember(Integer teamId, CustomUserDetails userDetails) {
        // 1. Check team có tồn tại không
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new BadRequestException("Team không tồn tại"));
        // 2. Check account đang đăng nhập có đag là thành viên của Team đó hay không
        Account currentAccount = userDetails.getAccount();
        TeamMember teamMember = teamMemberRepository.findByTeamAndStudent(team, currentAccount.getStudent())
                .orElseThrow(() -> new BadRequestException(
                        "Sinh viên hiện tại không thuộc Team này. Không được phép xem danh sách Team này."));
        // 3. Lấy danh sách teamMember
        List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
        TeamDetailResponse.MemberInfo leaderInfo = null;
        List<TeamDetailResponse.MemberInfo> officialMembers = new ArrayList<>();

        for (TeamMember member : teamMembers) {
            TeamDetailResponse.MemberInfo info = TeamDetailResponse.MemberInfo.builder()
                    .studentCode(member.getStudent().getStudentCode()).fullName(member.getStudent().getStudentName())
                    .email(member.getStudent().getAccount().getEmail()).major(member.getStudent().getMajor())
                    .isLeader(member.getIsLeader()).build();

            if (member.getIsLeader()) {
                leaderInfo = info;
            } else {
                officialMembers.add(info); // Chỉ add thành viên thường vào list này
            }
        }

        // 4. Lấy danh sách các email đã gửi lời mời
        List<TeamDetailResponse.InviteInfo> inviteInfo = new ArrayList<>();
        // Chỉ khi người đang xem là LEADER thì mới xem được lời mời
        if (teamMember.getIsLeader()) {
            List<Notification> invites = notificationRepository.findByTeamAndType(team,
                    NotificationType.TEAM_INVITATION);
            for (Notification invite : invites) {
                inviteInfo.add(
                        new TeamDetailResponse.InviteInfo(invite.getAccount().getEmail(), invite.getStatus().name()));
            }
        }

        return TeamDetailResponse.builder().teamId(team.getTeamId()).teamName(team.getTeamName()).leader(leaderInfo)
                .members(officialMembers).createAt(team.getCreateAt()).invitations(inviteInfo).build();
    }

    // Leader xem thông tin về hạng mục thi của đội
    @Override
    // Tổng hợp sự kiện, vòng thi và kết quả thi đấu hiện tại của đội sinh viên.
    public TeamCompetitionResponse getTeamCompetition(CustomUserDetails userDetails) {
        // Check leader
        Account account = userDetails.getAccount();
        if (account.getStudent() == null) {
            throw new BadRequestException("Tài khoản của bạn không liên kết với thông tin sinh viên nào.");
        }
        Student student = studentRepository.findById(account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin sinh viên"));
        TeamMember teamMember = student.getTeamMembers().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Bạn chưa tham gia vào bất kì Team nào."));
        if (!teamMember.getIsLeader()) {
            throw new BadRequestException(
                    "Bạn không phải là leader nên không được phép xem thông tin về hạng mục này.");
        }
        Team team = teamMember.getTeam();
        if (team == null) {
            throw new BadRequestException("Không tìm thấy thông tin  về Team này");
        }

        List<CategoryRound> categoryRound = team.getRegistrations().stream()
                .filter(registration -> registration != null && registration.getStatus() == RegistrationStatus.APPROVED)
                .filter(registration -> registration.getParticipants() != null).map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(participant -> participant != null && participant.getStatus() == ParticipantStatus.ACTIVE)
                .map(TeamParticipant::getCategoryRound).filter(cr -> cr != null && cr.getRound() != null)
                .sorted(Comparator
                        .comparing(cr -> cr.getRound().getOrderIndex() != null ? cr.getRound().getOrderIndex() : 0)) // Sắp
                                                                                                                     // xếp
                                                                                                                     // theo
                                                                                                                     // thứ
                                                                                                                     // tự
                                                                                                                     // Vòng
                                                                                                                     // 1,
                                                                                                                     // Vòng
                                                                                                                     // 2,
                                                                                                                     // Vòng
                                                                                                                     // Final
                .toList();
        if (categoryRound.isEmpty()) {
            throw new BadRequestException("Đội của bạn hiện không tham gia vòng thi nào hoặc chưa được kích hoạt.");
        }

        String eventName = "N/A";
        if (categoryRound.getFirst().getRound().getHackathonEvent() != null) {
            eventName = categoryRound.getFirst().getRound().getHackathonEvent().getEventName();
        }

        List<TeamCompetitionResponse.RoundInfo> roundInfoList = new ArrayList<>();

        for (CategoryRound category : categoryRound) {
            String roundName = category.getRound().getRoundName();
            String categoryName = category.getCategory().getCategoryName();
            // Check vòng thi có tồn tại chưa

            TeamCompetitionResponse.RoundInfo roundExist = null;
            for (TeamCompetitionResponse.RoundInfo roundInfo : roundInfoList) {
                if (roundInfo.getRoundName().equalsIgnoreCase(roundName)) {
                    roundExist = roundInfo;
                    break;
                }

            }
            if (roundExist == null) {
                TeamCompetitionResponse.RoundInfo newRoundInfo = new TeamCompetitionResponse.RoundInfo();
                newRoundInfo.setRoundName(roundName);
                //
                List<TeamCompetitionResponse.Category> categories = new ArrayList<>();
                categories.add(new TeamCompetitionResponse.Category(categoryName));
                newRoundInfo.setCategories(categories);
                roundInfoList.add(newRoundInfo);
            } else {
                boolean catExists = roundExist.getCategories().stream()
                        .anyMatch(c -> c.getCategoryName().equalsIgnoreCase(categoryName));
                if (!catExists) {
                    roundExist.getCategories().add(new TeamCompetitionResponse.Category(categoryName));
                }
            }
        }

        return TeamCompetitionResponse.builder().eventName(eventName).teamId(team.getTeamId())
                .teamName(team.getTeamName()).rounds(roundInfoList).build();
    }

    // FUNCTION ADMIN QUẢN LÝ LIST THÔNG TIN TEAM
    @Override
    // Lấy toàn bộ đội cùng thành viên để quản trị viên theo dõi và quản lý.
    public List<TeamDetailResponse> getTeamForAdmin(CustomUserDetails userDetails) {
        // 1. Check admin
        Account account = userDetails.getAccount();
        if (account.getRole() != AccountRole.ADMIN) {
            throw new BadRequestException("Bạn không phải là Admin, bạn không được phép xem danh sách này.");
        }

        // 2. Lấy list team
        List<Team> listTeam = teamRepository.findAll();
        List<TeamDetailResponse.MemberInfo> members = new ArrayList<>();
        TeamDetailResponse.MemberInfo leader = null;
        return listTeam.stream().map(team -> {
            String leaderName = team.getTeamMembers().stream().filter(TeamMember::getIsLeader) // Lọc người có isLeader
                                                                                               // == true
                    .map(tm -> tm.getStudent().getStudentName()).findFirst().orElse("Chưa có Leader");
            TeamDetailResponse.MemberInfo leaderInfo = TeamDetailResponse.MemberInfo.builder().fullName(leaderName)
                    .build();
            int size = team.getTeamMembers() != null ? team.getTeamMembers().size() : 0;
            String statusStr = team.getStatus().name();
            return TeamDetailResponse.builder().teamId(team.getTeamId()).teamName(team.getTeamName()).leader(leaderInfo)
                    .createAt(team.getCreateAt()).sizeTeam(size).status(statusStr).build();
        }).collect(Collectors.toList());
    }

    // FUNCTION EXPERT XEM THÔNG TIN CÁC ĐỘI THI MÀ MÌNH QUẢN LÝ
    @Override
    // Lấy chi tiết một đội cụ thể theo mã đội và quyền của người yêu cầu.
    public TeamDetailResponse getTeamDetail(Integer teamId, CustomUserDetails userDetails) {
        // 1. Check admin
        Account account = userDetails.getAccount();
        if (account.getRole() != AccountRole.EXPERT && account.getRole() != AccountRole.EVENTCOORDINATOR) {
            throw new BadRequestException(
                    "Bạn không có quyền  xem danh sách này. Chỉ có  EVENT COORDINATOR , EXPERT mới có thể xem.");
        }
        // 2. Coordinator được xem ds này
        if (teamId == null) {
            throw new BadRequestException("Hãy cung cấp ID của TEAM để xem danh sách chi tiết của Team. ");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về Team này."));
        if (account.getRole() == AccountRole.EXPERT) {
            // Nếu là EventCoordinator muốn xem thông tin expert quản lý phải nhập id tương
            // ứng cuar họ
            Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId()).orElseThrow(
                    () -> new BadRequestException("Không tìm thấy thông tin Chuyên gia tương ứng với tài khoản này."));

            // 1. Lấy danh sách các CategoryRoundId mà Mentor được phân công quản lý
            List<Integer> categoryRoundId = expert.getExpertAssigns().stream()
                    .filter(expertAssign -> expertAssign.getRole().equals(ExpertRole.MENTOR))
                    .map(ExpertAssign::getCategoryRound)
                    .filter(categoryRound -> categoryRound != null && categoryRound.getCategory() != null)
                    .map(CategoryRound::getCategoryRoundId).toList();
            if (categoryRoundId.isEmpty()) {
                throw new BadRequestException(
                        "Bạn chưa được phân công quản lý hạng mục nào , không thể xem chi tiết đội thi.");
            }
            // Kiểm tra Team có thuộc CategoryRound mà Mentor được phân công hay không

            CategoryRound currentTeamRound = team.getRegistrations().stream().map(Registration::getParticipants)
                    .filter(p -> p != null && !p.isEmpty()).flatMap(List::stream)
                    .filter(p -> p.getCategoryRound() != null).map(TeamParticipant::getCategoryRound).findFirst()
                    .orElseThrow(
                            () -> new BadRequestException("Đội thi này chưa được xếp vào vòng đấu hay hạng mục nào."));
            if (!categoryRoundId.contains(currentTeamRound.getCategoryRoundId())) {
                throw new BadRequestException("Bạn không có quyền xem chi tiết đội thi này ở vòng đấu hiện tại.");
            }

        }

        // 3. Lấy danh sách teamMember thông qua Team
        List<TeamDetailResponse.MemberInfo> memberList = new ArrayList<>();
        TeamDetailResponse.MemberInfo leaderInfo = null;
        if (team.getTeamMembers() != null && !team.getTeamMembers().isEmpty()) {

            for (TeamMember member : team.getTeamMembers()) {
                String avatar = (member.getStudent() != null && member.getStudent().getAccount() != null)
                        ? member.getStudent().getAccount().getAvatarUrl()
                        : null;

                TeamDetailResponse.MemberInfo info = TeamDetailResponse.MemberInfo.builder()
                        .fullName(member.getStudent().getStudentName())
                        .university(member.getStudent().getUniversityName()).major(member.getStudent().getMajor())
                        .avatarUrl(avatar).build();

                if (member.getIsLeader()) {
                    leaderInfo = info;
                } else {
                    memberList.add(info);
                }

            }

        }

        return TeamDetailResponse.builder().teamId(team.getTeamId()).teamName(team.getTeamName()).leader(leaderInfo)
                .members(memberList).build();

    }

    // MENTOR CÓ CÙNG HẠNG MỤC THỂ XEM THÔNG TIN CHUNG VỀ TEAM MÌNH DC PHÂN CÔNG
    // Lấy các đội liên quan đến một sự kiện để phục vụ ban tổ chức.
    public List<TeamDetailResponse> getTeamInfo(Integer eventId, CustomUserDetails userDetails) {

        // 1. Check coordinator , expert vs vai trò là mentor có thể xem.
        Account account = userDetails.getAccount();
        if (account.getRole() != AccountRole.EXPERT) {
            throw new BadRequestException(
                    "Bạn không có quyền  xem danh sách này. Chỉ có EXPERT với vai trò MENTOR mới có thể xem.");
        }
        HackathonEvent event = hackathonEventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về Event này."));

        List<Team> listTeam = new ArrayList<>();

        // TH2 Expert xem dc ds các Team mà các Expert khác quản lý nếu có cùng CATEGORY
        if (account.getRole() == AccountRole.EXPERT) {
            Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId()).orElseThrow(
                    () -> new BadRequestException("Không tìm thấy thông tin Expert tương ứng với account này."));
            // Lấy Category mà Expert này đang quản lý
            List<Integer> categoryRoundId = expert.getExpertAssigns().stream()
                    .filter(expertAssign -> expertAssign.getRole() == ExpertRole.MENTOR)
                    .map(ExpertAssign::getCategoryRound).filter(Objects::nonNull).map(CategoryRound::getCategoryRoundId)
                    .distinct().toList();

            if (categoryRoundId.isEmpty()) {
                throw new BadRequestException(
                        "Tài khoản Chuyên gia của bạn chưa được phân công vai trò MENTOR cho hạng mục nào.");
            }
            System.out.println("CategoryRoundIds = " + categoryRoundId);
            System.out.println("EventId = " + eventId);
            listTeam = teamRepository.findTeamsByCategoryRoundIdsAndEventId(categoryRoundId, eventId);
        }

        // 2. Lấy list team mà Expert quản lý
        // 3. Lấy danh sách teamMember thông qua Team
        List<TeamDetailResponse> list = new ArrayList<>();
        for (Team team : listTeam) {
            int count = team.getTeamSize();
            String categoryName = "N/A";
            String roundName = "N/A";

            Registration registration = team.getRegistrations().stream()
                    .filter(r -> r.getStatus() == RegistrationStatus.APPROVED).findFirst().orElse(null);

            if (registration != null) {
                List<TeamParticipant> participant = registration.getParticipants();
                for (TeamParticipant pt : participant) {
                    if (pt != null && pt.getCategoryRound() != null) {
                        CategoryRound cr = pt.getCategoryRound();
                        categoryName = (cr.getCategory() != null) ? cr.getCategory().getCategoryName() : "N/A";
                        roundName = (cr.getRound() != null) ? cr.getRound().getRoundName() : "N/A";

                        TeamDetailResponse response = TeamDetailResponse.builder().teamId(team.getTeamId())
                                .teamName(team.getTeamName()).sizeTeam(count).categoryName(categoryName)
                                .roundName(roundName).build();
                        list.add(response);
                    }
                }

            } else {
                TeamDetailResponse response = TeamDetailResponse.builder().teamId(team.getTeamId())
                        .teamName(team.getTeamName()).sizeTeam(count).categoryName("N/A").roundName("N/A").build();
                list.add(response);
            }

        }
        return list;
    }

    private TeamJoinResponse mapToTeamJoinResponse(TeamInvitation teamInvitation) {
        TeamJoinResponse response = new TeamJoinResponse();
        response.setRequestId(teamInvitation.getTeamInvitationId());
        response.setCreatedAt(teamInvitation.getCreatedAt());
        response.setTeamId(teamInvitation.getTeam().getTeamId());
        response.setStatus(teamInvitation.getStatus());
        response.setTeamName(teamInvitation.getTeam().getTeamName());
        response.setStudentId(teamInvitation.getAccount().getStudent().getStudentId());
        response.setStudentName(teamInvitation.getAccount().getStudent().getStudentName());
        response.setReason(teamInvitation.getReason());

        return response;
    }

    // Tìm đội đang hoạt động mà sinh viên hiện giữ vai trò trưởng nhóm.
    private Team getActiveTeamLedBy(Student student) {
        Team team = teamRepository.findCurrentTeamByStudentAndStatus(
                student.getStudentId(),
                List.of(TeamStatus.ACTIVE));

        if (team == null) {
            throw new BadRequestException("Bạn không thuộc team ACTIVE nào");
        }

        validateTeamLeader(team, student);
        return team;
    }

    // Xác nhận sinh viên là trưởng của đúng đội trước khi xử lý yêu cầu tham gia.
    private void validateTeamLeader(Team team, Student student) {
        if (team == null) {
            throw new BadRequestException("Team không tồn tại");
        }

        TeamMember teamMember = teamMemberRepository
                .findByTeamAndStudent(team, student)
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không thuộc team này"));

        if (!Boolean.TRUE.equals(teamMember.getIsLeader())) {
            throw new BadRequestException(
                    "Chỉ leader của team mới được xử lý yêu cầu tham gia");
        }
    }

    // Chỉ cho phép xử lý yêu cầu tham gia vẫn đang ở trạng thái chờ.
    private void validatePendingJoinRequest(TeamInvitation request) {
        if (request.getType() != InvitationType.JOIN_REQUEST) {
            throw new BadRequestException(
                    "Đây không phải yêu cầu tham gia team");
        }

        if (request.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý");
        }

        if (request.getTeam() == null || request.getAccount() == null) {
            throw new BadRequestException("Yêu cầu tham gia không hợp lệ");
        }
    }

}
