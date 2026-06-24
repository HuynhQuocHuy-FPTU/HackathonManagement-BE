package com.hackathon.service;

import com.hackathon.dto.TeamSelectionDTO;
import com.hackathon.dto.registration.RegistrationResponse;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ParticipantService participantService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    //1. Leader đại diện Team đăng ký cuộc thi
    @Override
    @Transactional
    public void registerEvent(Integer eventId, CustomUserDetails userDetails) {
        // Check thời hạn đăng ký cuộc thi
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về sự kiện này."));
//        if (LocalDateTime.now().isBefore(event.getStartDate())) {
//            throw new BadRequestException("Cuộc thi chưa mở cổng đăng ký! Vui lòng quay lại sau.");
//        }
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
        int countMember = team.getTeamSize();
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
    public List<RegistrationResponse> getTeamsForApproval(Integer evenId, CustomUserDetails userDetails) {
        Account currentAccount = userDetails.getAccount();
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Người dùng chưa đăng nhập tài khoản  hoặc phiên làm việc hết hạn.");
        }
        if (currentAccount.getRole() != AccountRole.EVENTCOORDINATOR) {
            throw new BadRequestException("Bạn không phải là EventCoordinator nên không được phép truy cập tính năng phê duyệt thành viên này.");
        }
        HackathonEvent event = eventRepository.findById(evenId).orElseThrow(
                () -> new BadRequestException("Không tìm thấy thông tin về sự kiện này."));

        // 2. Lấy ds Team đang chờ phê duyệt PENDING
        List<Registration> pendingRegistrations = registrationRepository
                .findByHackathonEvent_EventIdAndStatus(evenId, RegistrationStatus.PENDING);

        List<RegistrationResponse> pendingList = new ArrayList<>();
        for (Registration regis : pendingRegistrations) {
            Team team = regis.getTeam();
            RegistrationResponse reponse = RegistrationResponse.builder()
                    .registrationId(regis.getRegistrationId())
                    .teamId(regis.getTeam().getTeamId())
                    .teamName(regis.getTeam().getTeamName())
                    .build();
            pendingList.add(reponse);

        }

        return pendingList;
    }

    //Coordinator duyệt Registration — chuyển trạng thái sang APPROVED.

    @Override
    @Transactional
    public Registration approveRegistration(Integer registrationId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Registration: " + registrationId));

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể duyệt Registration ở trạng thái PENDING");
        }
        //Cập nhật trạng thái Registration
        registration.setStatus(RegistrationStatus.APPROVED);
        registration = registrationRepository.save(registration);

        //cập nhật trạng thái của team
        Team team = registration.getTeam();
        if (team != null) {
            team.setStatus(TeamStatus.BUSY);
        }

        //tạo participant lưu các team đã được approve trước
        participantService.saveParticipant(registration);
        auditService.saveLog(
                account,
                AuditAction.APPROVE_REGISTRATION,
                AuditEntityType.REGISTRATION,registrationId,
                "Approve registration of team:  " + registration.getTeam().getTeamName()
        );
        TeamMember leader = registration.getTeam()
                .getTeamMembers()
                .stream()
                .filter(TeamMember::getIsLeader)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy team leader"));
        Account leaderAccount = leader.getStudent().getAccount();
        notificationService.notifyRegistrationApproved(account, leaderAccount, registration.getTeam().getTeamName(), registration.getHackathonEvent().getEventName());
        return registration;
    }

    //Coordinator từ chối Registration - Chuyển trạng thái sang REJECTED
    @Override
    @Transactional
    public Registration rejectRegistration(Integer registrationId, String reason) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Registration: " + registrationId));

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể từ chối Registration ở trạng thái PENDING");
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        registration = registrationRepository.save(registration);
        auditService.saveLog(
                account,
                AuditAction.REJECT_REGISTRATION,
                AuditEntityType.REGISTRATION,registrationId,
                "Reject registration of team:  " + registration.getTeam().getTeamName()
        );
        TeamMember leader = registration.getTeam()
                .getTeamMembers()
                .stream()
                .filter(TeamMember::getIsLeader)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy team leader"));
        Account leaderAccount = leader.getStudent().getAccount();
        notificationService.notifyRegistrationRejected(account, leaderAccount, registration.getTeam().getTeamName(), registration.getHackathonEvent().getEventName(), reason);
        return registration;
    }

    @Override
    public List<TeamSelectionDTO> getApprovedRegistrations(Integer eventId) {
        //1. Lấy danh sách registration đã approve
        List<Registration> registrations =  registrationRepository.findByHackathonEvent_EventIdAndStatus(eventId, RegistrationStatus.APPROVED);
        //2. Map sang DTO
        return registrations.stream().map(reg -> new TeamSelectionDTO(reg.getRegistrationId(), reg.getTeam().getTeamName())).toList();
    }

    @Override
    public List<Registration> getRegistrationsToCancelled(Integer eventId) {
        List<RegistrationStatus> list = List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);
        List<Registration> registrations =  registrationRepository.findRegistrationByHackathonEvent_EventIdAndStatusIn(eventId, list);
        return registrations;
    }

    @Override
    public void transferStatusToRejectd(List<Registration> registrations) {
        for(Registration registration: registrations){
            registration.setStatus(RegistrationStatus.REJECTED);
        }
    }


    @Override
    public RegistrationResponse getTeamsDetailForApproval(Integer registrationId, CustomUserDetails userDetails) {
        Account currentAccount = userDetails.getAccount();
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Người dùng chưa đăng nhập tài khoản  hoặc phiên làm việc hết hạn.");
        }
        if (currentAccount.getRole() != AccountRole.EVENTCOORDINATOR) {
            throw new BadRequestException("Bạn không phải là EventCoordinator nên không được phép truy cập tính năng phê duyệt thành viên này.");
        }

        // 2. Lấy thông tin Team đang chờ phê duyệt PENDING
        Registration regis = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn đăng ký"));

        Team team = regis.getTeam();
        int count = team.getTeamSize();
        RegistrationResponse.MemberInfo leader = null;
        List<RegistrationResponse.MemberInfo> members = new ArrayList<>();

        if (team != null) {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeam(team);
            for (TeamMember memberInfo : teamMembers) {
                RegistrationResponse.MemberInfo info = new RegistrationResponse.MemberInfo(
                        memberInfo.getStudent().getStudentCode(),
                        memberInfo.getStudent().getStudentName(),
//                        memberInfo.getStudent().getUniversityName(),
                        memberInfo.getStudent().getMajor(),
                        memberInfo.getStudent().getAccount().getEmail()
                );
                if (memberInfo.getIsLeader()) {
                    leader = info;
                } else {
                    members.add(info);
                }

            }

        }

        return new RegistrationResponse(
                regis.getRegistrationId(),
                regis.getHackathonEvent().getEventName(),
                team.getTeamId(),
                team.getTeamName(),
                regis.getRegistrationDate(),
                count,
                leader,
                members
        );
    }

}
