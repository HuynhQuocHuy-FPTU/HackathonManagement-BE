package com.hackathon.service;

import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.entity.enums.TeamStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationEventServiceImpl implements RegistrationEventService {
    private final HackathonEventRepository eventRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final TeamRepository teamRepository;
    private final AccountRepository accountRepository;
    //    private final NotificationRepository notificationRepository;

    //1. Leader đại diện Team đăng ký cuộc thi
    @Override
    @Transactional
    public void registerEvent(Integer eventId, CustomUserDetails userDetails) {
        // Check thời hạn đăng ký cuộc thi
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về sự kiện này."));
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Đã quá hạn đăng ký tham gia cuộc thi này!");
        }

        //1. Check Team
        //2. Check leader
        Account currentAccount = userDetails.getAccount();
        TeamMember leaderRecord = teamMemberRepository.findByStudentAndIsLeader(userDetails.getAccount().getStudent(), true)
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Leader của đội nào, không thể đăng ký!"));

        Team team = leaderRecord.getTeam();
        if (leaderRecord.getStudent().getStudentId() != (currentAccount.getStudent().getStudentId())) {
            throw new BadRequestException("Bạn không phải là Leader, bạn không được phép đăng ký event");

        }

        //3. Check status hiện tại của Team(Draf, pending, approve)
        Optional<Registration> registrationEvent = registrationRepository.findByTeamAndHackathonEvent_EventId(team, event.getEventId());

        if (registrationEvent.isPresent()) {
            Registration reg = registrationEvent.get();

            if (reg.getStatus().equals(RegistrationStatus.PENDING)) {
                throw new BadRequestException("Đội của bạn đã gửi đơn cho sự kiện này rồi. Xin hãy chờ phê duyệt!");
            }
            if (reg.getStatus().equals(RegistrationStatus.APPROVED)) {
                throw new BadRequestException("Đội của bạn đã được phê duyệt cho sự kiện này rồi.");
            }
            if (reg.getStatus().equals(RegistrationStatus.REJECTED)) {
                throw new BadRequestException("Đội của bạn đã bị từ chối.Vui lòng kiểm tra lại thông tin đăng ký.");
            }
        }
        //4.Check số lượng thành viên
        int countMember = team.getTeamSize() ;

        if (countMember < event.getMinTeamSize()) {
            throw new BadRequestException("Bạn không thể đăng ký cuộc thi. Số lượng thành viên tối thiểu bắt buộc phải lớn hơn hoặc bằng " + event.getMinTeamSize() +
                    " .Thành viên chính thức hiện tại bạn đang sở hữu là " + countMember);
        }
        if (countMember > event.getMaxTeamSize()) {
            throw new BadRequestException("Bạn không thể đăng ký cuộc thi. Số lượng thành viên tối đa của cuộc thi này là: " + event.getMaxTeamSize() +
                    " .Thành viên chính thức hiện tại bạn đang sở hữu là " + countMember);
        }


        // 5. Tạo bảng registration để lưu thông tin đăng ký
        Registration registration = new Registration();
        registration.setHackathonEvent(event);
        registration.setTeam(team);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setStatus(RegistrationStatus.PENDING);
        registrationRepository.save(registration);

        team.setStatus(TeamStatus.PENDING);
        teamRepository.save(team);
    }

    //2. Lấy thông tin của all Team đk event để Coordinator phê duyệt
    @Override
    public List<TeamResponse>  getTeamsForApproval(Integer evenId, CustomUserDetails userDetails) {
        Account currentAccount = userDetails.getAccount();
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Người dùng chưa đăng nhập tài khoản  hoặc phiên làm việc hết hạn.");
        }
        if (currentAccount.getRole() != AccountRole.EVENTCOORDINATOR){
            throw new BadRequestException("Bạn không phải là EventCoordinator nên không được phép truy cập tính năng phê duyệt thành viên này.");
        }

        // 2. Lấy ds Team đang chờ phê duyệt PENDING
        List<Registration> pendingRegistrations = registrationRepository
                .findByHackathonEvent_EventIdAndStatus(evenId, RegistrationStatus.PENDING);

        List<TeamResponse> pendingList = new ArrayList<>();
        for (Registration regis: pendingRegistrations ){
            Team team = regis.getTeam();
            List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
            TeamResponse.MemberInfo leaderInfo = null;
            List<TeamResponse.MemberInfo> officialMembers = new ArrayList<>();
            for (TeamMember member : teamMembers) {
                TeamResponse.MemberInfo info = new TeamResponse.MemberInfo(
                        member.getStudent().getStudentCode(),
                        member.getStudent().getStudentName(),
                        member.getStudent().getAccount().getEmail(),
                        member.getStudent().getMajor()
                );

                if (member.getIsLeader()) {
                    leaderInfo = info;
                } else {
                    officialMembers.add(info);
                }
            }

            TeamResponse response = TeamResponse.builder()
                    .teamId(team.getTeamId())
                    .teamName(team.getTeamName())
                    .leader(leaderInfo)
                    .members(officialMembers)
                    .createAt(team.getCreateAt())
                    .build();

            pendingList.add(response);

        }
            return pendingList;
    }


}
