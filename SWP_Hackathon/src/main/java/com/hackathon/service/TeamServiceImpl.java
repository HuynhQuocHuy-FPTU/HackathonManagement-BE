package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamResponse;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.TeamStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static javax.management.Query.or;

@Service
@RequiredArgsConstructor

public class TeamServiceImpl implements TeamService {
    private final HackathonEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final AccountRepository accRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationRepository notificationRepository;

    //FUNCTION 1:Create Team
    //BR: Khi tao team phai co tieu thieu it nhat 1 thanh vien (bao gom leader va 1 thanh vien khac)
    private static final int MAX_TEAM_SIZE = 5;

    @Transactional
    @Override
    public TeamResponse createTeam(CreateTeamRequest request) {
        //Check Deadline Registration createTeam
        checkTeamRegistrationWindow();

        // 1. Lay thong tin cua leader(Nguoi tao tem se duoc gan role la leader)
        // (Bo sung sau, sua lai khi co account)
        String leaderEmail = getCurrentUserEmail();
        Account leader = accRepository.findByEmail(request.getTeamName());
        if (leader == null) {
            throw new BadRequestException("Account not found");
        }

        // 2. Check member list
        if (request.getMemberEmails() == null || request.getMemberEmails().isEmpty()) {
            throw new BadRequestException("Team must at least 1 member besides leader");
        }

        // Check duplicate member
        Set<String> set = new HashSet<>(request.getMemberEmails());
        for (String email : request.getMemberEmails()) {
            if (email != null) {
                set.add(email.trim());
            }
        }
        if (set.size() != request.getMemberEmails().size()) {
            throw new BadRequestException("Duplicate members");
        }
        // Check Email cua Leader vs Email cua listMember
        if (set.contains(leaderEmail)) {
            throw new BadRequestException("The leader cannot add email addresses to the list of members");
        }

        // Check Team toi da 5 member (leader + 4 member)
        int totalMember = 1 + request.getMemberEmails().size();
        if (totalMember > MAX_TEAM_SIZE) {
            throw new BadRequestException("Team size exceeds the maximum allowed size of " + MAX_TEAM_SIZE);
        }

        //3. Create Team
        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setStatus(TeamStatus.PENDING);
        team.setTeamSize(totalMember);
        Team saveTeam = teamRepository.save(team);

        List<TeamResponse.MemberInfo> listMember = new ArrayList<>();

        //4. Luu thong tin Leader
        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(saveTeam);
        leaderMember.setIsLeader(true);
        leaderMember.setStudent(leader.getStudent());
        teamMemberRepository.save(leaderMember);

        //5. Tạo object, save info of leader vao ListMember
        TeamResponse.MemberInfo leaderInfo = new TeamResponse.MemberInfo(leader.getStudent().getStudentCode(), leader.getStudent().getStudentName(), leader.getStudent().getAccount().getEmail());
        listMember.add(leaderInfo);

        //6. Luu cac thong tin member vao ListMember
        for (String memberEmail : set) {
            Account memberAccount = accRepository.findByEmail(memberEmail.trim());
            if (memberAccount == null || memberAccount.getEmail().isEmpty()) {
                throw new RuntimeException("Member Account not found");
            }
            //Luu thong tin Member
            TeamMember studentMember = new TeamMember();
            studentMember.setTeam(saveTeam);
            studentMember.setIsLeader(false);
            studentMember.setStudent(memberAccount.getStudent());
            teamMemberRepository.save(studentMember);

            //Map DTO
            TeamResponse.MemberInfo memberInfo = new TeamResponse.MemberInfo(memberAccount.getStudent().getStudentCode(), memberAccount.getStudent().getStudentName(), memberAccount.getStudent().getAccount().getEmail());
            listMember.add(memberInfo);
        }

        //6. Return TeamResponse
        return new TeamResponse(saveTeam.getTeamId(), saveTeam.getTeamName(), leaderInfo, listMember, saveTeam.getCreateAt());


    }

    //FUNCTION 2:UPDATE INFORMATION ABOUT TEAM AS NAME
    @Override
    @Transactional
    public TeamResponse updateInfo(CreateTeamRequest request) {
        //1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        String email = getCurrentUserEmail();
        Account currentUser = accRepository.findByEmail(email);
        if (currentUser == null) {
            throw new BadRequestException("Account not found");
        }

        // 2. Check leader(Check account student đang login có phải là leader ko )
        TeamMember leader = teamMemberRepository.findByStudent_AccountAndIsLeaderTrue(currentUser).orElseThrow(() -> new BadRequestException("Only Leader can update info"));

        // Lấy team mà Leader quản lý
        Team team = leader.getTeam();

        // 3.Check deadline(chi duoc update truoc khi het han dk tao team )
        checkTeamRegistrationWindow();

        //4. Check TeamName duplicate
        String newTeamName = request.getTeamName();
        if (newTeamName != null && !newTeamName.isBlank()) {

            if (!team.getTeamName().equalsIgnoreCase(newTeamName) && teamRepository.existsByTeamName(newTeamName)) {
                throw new BadRequestException("Team name already exists");
            }

            team.setTeamName(newTeamName.trim());
        }

        // 4. Update
        teamRepository.save(team);
        TeamResponse response = new TeamResponse();
        response.setTeamName(team.getTeamName());
        return response;
    }

    //FUNCTION 3: RỜI TEAM
    //BR-03: Member chỉ được phép Leave team trước khi chốt danh sách 24 giờ
    @Override
    public void leaveTeam(Integer teamId) {
        //1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        String email = getCurrentUserEmail();
        Account currentUser = accRepository.findByEmail(email);
        if (currentUser == null) {
            throw new BadRequestException("Account not found");
        }
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        //2. Check Student co thuoc team ko or con trong nhom ko
        TeamMember teamMember = teamMemberRepository.findByTeamAndStudent(teamId, currentUser.getStudent()).orElseThrow(() -> new RuntimeException("You are not a member of this group, so you cannot leave team"));

        //3. Leader khong duoc phep roi khoi nhom , truoc khi chuyen quyen leader cho nguoi khac
        if (teamMember.getIsLeader()) {
            throw new BadRequestException("Leader cannot leave team.The leader needs to transfer authority before leaving the team.");
        }

        //4. Check thoi han duoc roi khoi team
        Optional<Registration> registrationOpt = registrationRepository.findByTeam(team);

        // check Team da dang ky event ch
        if (registrationOpt.isPresent()) {
            //Th1: Neu team da gui don dang ky cuoc thi  roi, thi khong duoc phep roi Team
            if (team.getStatus() == TeamStatus.PENDING || team.getStatus() == TeamStatus.APPROVED) {
                throw new BadRequestException("Team already submitted, cannot leave anymore");
            }
        } else {
            //Th2: Neu Team ch gui don dang ky cuoc thi, van co the roi nhom nhung truoc khi cong dk Team dong truoc 24h
            List<HackathonEvent> event = eventRepository.findByStatus(EventStatus.ACTIVE);
            LocalDateTime lockTime = event.getFirst().getRegistrationDeadline().minusHours(24);
            if (LocalDateTime.now().isAfter(lockTime)) {
                throw new BadRequestException("Cannot leave team within 24 hours before deadline");
            }

        }
        teamMemberRepository.delete(teamMember);

        //5. Cập nhật lại số lượng thành viên của nhóm
        team.setTeamSize(team.getTeamSize() - 1);
        teamRepository.save(team);

    }

    //FUNCTION 4: CHẤP NHẬN LỜI MỜI
    // Chuyen quyen leader
    @Override
    public void acceptInvite(Integer teamId, Long notificationId) {
        //1. Lấy thông tin người dùng hiện đang đăng nhập từ JWT/OAuth2.
        String email = getCurrentUserEmail();
        Account currentUser = accRepository.findByEmail(email);
        if (currentUser == null) {
            throw new BadRequestException("Account not found");
        }

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
        int currentSize= Optional.ofNullable(team.getTeamSize()).orElse(0);
        if (currentSize  >= 5) {

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
            // Tìm tất cả thông báo chứa "teamId=X" của cuộc thi này
            List<Notification> otherInvites = notificationRepository.findAllByTeam( team);

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


    // Sua lai khi co ham login de lay thong tin ng dang nhap
    @Override
    public String getCurrentUserEmail() {
        return "";
    }

    @Override
    public void checkTeamRegistrationWindow() {
        // Check event co hoat dong khong
        List<HackathonEvent> event = eventRepository.findByStatus(EventStatus.ACTIVE);
        if (event.isEmpty()) {
            throw new IllegalArgumentException("No active hackathon event found");
        }
        if (LocalDateTime.now().isAfter(event.getFirst().getRegistrationDeadline())) {
            throw new BadRequestException("Deadline passed, cannot update team");
        }
    }


}
