package com.hackathon.service;

import com.hackathon.dto.TeamSelectionDTO;
import com.hackathon.dto.registration.CountRegistrationDTO;
import com.hackathon.dto.registration.RegistrationResponse;
import com.hackathon.dto.registration.RegistrationHistoryResponse;
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

    @Override
    @Transactional
    public List<RegistrationHistoryResponse> getCurrentTeamRegistrationHistory(
            CustomUserDetails userDetails
    ) {
        Account account = userDetails.getAccount();
        if (account == null || account.getStudent() == null) {
            throw new BadRequestException("Chỉ tài khoản sinh viên mới có thể xem lịch sử đăng ký của đội.");
        }

        Team currentTeam = teamRepository.findCurrentTeamByStudentAndStatus(
                account.getStudent().getStudentId(),
                List.of(TeamStatus.BUSY, TeamStatus.DRAFT, TeamStatus.PENDING)
        );
        if (currentTeam == null) {
            throw new BadRequestException("Bạn không thuộc đội nào đang hoạt động.");
        }

        return registrationRepository
                .findByTeam_TeamIdOrderByRegistrationDateDesc(currentTeam.getTeamId())
                .stream()
                .map(registration -> RegistrationHistoryResponse.builder()
                        .registrationId(registration.getRegistrationId())
                        .eventId(registration.getHackathonEvent().getEventId())
                        .eventName(registration.getHackathonEvent().getEventName())
                        .teamId(currentTeam.getTeamId())
                        .teamName(currentTeam.getTeamName())
                        .teamSize(currentTeam.getTeamSize())
                        .registrationDate(registration.getRegistrationDate())
                        .status(registration.getStatus())
                        .build())
                .toList();
    }

    //1. Leader đại diện Team đăng ký cuộc thi
    @Override
    @Transactional
    public void registerEvent(Integer eventId, CustomUserDetails userDetails) {
        // Check thời hạn đăng ký cuộc thi
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về sự kiện này."));

        if(event.getStatus() == EventStatus.DRAFT){
            throw new BadRequestException("Sự kiện chưa được công bố không thể đăng ký cuộc thi.");
        }
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
                reg.setStatus(RegistrationStatus.PENDING);
                reg.setRegistrationDate(LocalDateTime.now());

                registrationRepository.save(reg);
                team.setStatus(TeamStatus.PENDING);
                teamRepository.save(team);

                return;
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

        // 5. Check có account github chưa , nếu null ko cho đk
        if (currentAccount.getGithubId() == null) {
            throw new BadRequestException(
                    "Bạn chưa liên kết tài khoản GitHub. " +
                            "Bạn cần phải tạo tài khoản GitHub trước khi đăng ký tham gia sự kiện.");
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
        auditService.saveLog(
                currentAccount,
                AuditAction.REGISTER_EVENT,
                AuditEntityType.REGISTRATION,
                registration.getRegistrationId(),
                "Đăng ký tham gia sự kiện thành công"
        );
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
            TeamMember leader = teamMemberRepository.findLeaderByTeamId(regis.getTeam().getTeamId());
            RegistrationResponse.MemberInfo info = new RegistrationResponse.MemberInfo(
                    leader.getStudent().getStudentCode(),
                    leader.getStudent().getStudentName(),
                    leader.getStudent().getMajor(),
                    leader.getStudent().getAccount().getEmail());
            RegistrationResponse reponse = RegistrationResponse.builder()
                    .registrationId(regis.getRegistrationId())
                    .teamId(regis.getTeam().getTeamId())
                    .teamName(regis.getTeam().getTeamName())
                    .leader(info)
                    .build();
            pendingList.add(reponse);

        }

        return pendingList;
    }

    @Override
    public List<TeamSelectionDTO> getAllTeamRegistrations(Integer evenId) {
        List<Registration> registrations =  registrationRepository.findAll();
        return registrations.stream().map(reg -> new TeamSelectionDTO(reg.getRegistrationId(), reg.getTeam().getTeamName())).toList();
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

        HackathonEvent event = eventRepository
                .findByIdForRegistrationApproval(
                        registration.getHackathonEvent().getEventId()
                )
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy event của Registration"));

        validateWorkshopHasNotStarted(event);

        Integer maxTeam = event.getMaxTeam();
        if (maxTeam == null || maxTeam < 1) {
            throw new BadRequestException(
                    "Event chưa cấu hình số lượng đội tối đa hợp lệ");
        }

        long approvedTeamCount =
                registrationRepository.countByHackathonEvent_EventIdAndStatus(
                        event.getEventId(),
                        RegistrationStatus.APPROVED
                );

        if (approvedTeamCount >= maxTeam) {
            throw new BadRequestException(
                    "Event đã đủ số lượng đội tối đa: " + maxTeam);
        }

        Team team = registration.getTeam();
        if (team == null) {
            throw new BadRequestException(
                    "Registration không liên kết với team");
        }

        validateNoOverlappingApprovedEvent(
                team,
                registration.getHackathonEvent()
        );

        //Cập nhật trạng thái Registration
        registration.setStatus(RegistrationStatus.APPROVED);
        registration = registrationRepository.saveAndFlush(registration);

        //cập nhật trạng thái của team
        team.setStatus(TeamStatus.BUSY);
        teamRepository.save(team);

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

    private void validateWorkshopHasNotStarted(HackathonEvent event) {
        if (event.getWorkshopTime() == null) {
            throw new BadRequestException(
                    "Event chưa cấu hình thời gian workshop"
            );
        }

        if (event.getWorkshopStatus() != WorkshopStatus.UPCOMING
                || !LocalDateTime.now().isBefore(event.getWorkshopTime())) {
            throw new BadRequestException(
                    "Chỉ có thể duyệt registration trước khi workshop bắt đầu"
            );
        }
    }

    private void validateNoOverlappingApprovedEvent(
            Team team,
            HackathonEvent targetEvent
    ) {
        if (targetEvent == null
                || targetEvent.getStartDate() == null
                || targetEvent.getEndDate() == null) {
            throw new BadRequestException(
                    "Event chưa cấu hình đầy đủ thời gian bắt đầu và kết thúc");
        }

        List<Registration> approvedRegistrations =
                registrationRepository.findByTeam_TeamIdAndStatus(
                        team.getTeamId(),
                        RegistrationStatus.APPROVED
                );

        boolean hasOverlap = approvedRegistrations.stream()
                .map(Registration::getHackathonEvent)
                .filter(event -> event != null
                        && event.getStartDate() != null
                        && event.getEndDate() != null)
                .filter(event -> event.getEventId()
                        != targetEvent.getEventId())
                .anyMatch(event ->
                        event.getStartDate().isBefore(targetEvent.getEndDate())
                        && targetEvent.getStartDate().isBefore(event.getEndDate())
                );

        if (hasOverlap) {
            throw new BadRequestException(
                    "Team đã được duyệt tham gia một event khác trùng thời gian");
        }
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
    public CountRegistrationDTO getCountRegistrations(Integer eventId) {
        Integer countApproved = registrationRepository.countRegistration(RegistrationStatus.APPROVED);

        Integer countRejected = registrationRepository.countRegistration(RegistrationStatus.REJECTED);

        Integer countPending = registrationRepository.countRegistration(RegistrationStatus.PENDING);

        return CountRegistrationDTO.builder().countApproved(countApproved).countReject(countRejected).countPending(countPending).build();
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
