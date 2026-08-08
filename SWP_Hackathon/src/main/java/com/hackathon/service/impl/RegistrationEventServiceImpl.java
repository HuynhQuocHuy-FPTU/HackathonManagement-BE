package com.hackathon.service.impl;

import com.hackathon.dto.TeamSelectionDTO;
import com.hackathon.dto.registration.CountRegistrationDTO;
import com.hackathon.dto.registration.RegistrationResponse;
import com.hackathon.dto.registration.RegistrationHistoryResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import com.hackathon.service.ParticipantService;
import com.hackathon.service.RegistrationEventService;
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
// Xử lý việc đăng ký sự kiện, xét duyệt đội và theo dõi lịch sử đăng ký.
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
    // Lấy lịch sử đăng ký sự kiện của đội hiện tại do sinh viên đang tham gia.
    public List<RegistrationHistoryResponse> getCurrentTeamRegistrationHistory(
            CustomUserDetails userDetails
    ) {
        // Lấy tài khoản trực tiếp từ phiên đăng nhập hiện tại.
        Account account = userDetails.getAccount();
        // Chỉ sinh viên mới có quan hệ thành viên đội để xem lịch sử đăng ký.
        if (account == null || account.getStudent() == null) {
            throw new BadRequestException("Chỉ tài khoản sinh viên mới có thể xem lịch sử đăng ký của đội.");
        }

        // Tìm đội hiện tại của sinh viên trong các trạng thái vẫn còn được hệ thống quản lý.
        Team currentTeam = teamRepository.findCurrentTeamByStudentAndStatus(
                account.getStudent().getStudentId(),
                List.of(TeamStatus.BUSY, TeamStatus.ACTIVE, TeamStatus.PENDING)
        );
        // Không thể xác định lịch sử theo đội nếu sinh viên không thuộc đội đang hoạt động.
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
    // Kiểm tra đội, thời gian và điều kiện tham dự trước khi tạo đăng ký sự kiện.
    public void registerEvent(Integer eventId, CustomUserDetails userDetails) {
        // Tìm sự kiện cần đăng ký và dừng ngay nếu mã sự kiện không tồn tại.
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về sự kiện này."));

        // Sự kiện ở trạng thái nháp chưa được phép tiếp nhận đăng ký.
        if(event.getStatus() == EventStatus.DRAFT){
            throw new BadRequestException("Sự kiện chưa được công bố không thể đăng ký cuộc thi.");
        }
        // Không tạo đăng ký mới sau thời điểm đóng cổng đăng ký của sự kiện.
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Đã quá hạn đăng ký tham gia cuộc thi này!");
        }

        // Lấy tài khoản dùng để kiểm tra quyền trưởng nhóm và ghi lịch sử thao tác.
        Account currentAccount = userDetails.getAccount();
        // Tìm bản ghi thành viên xác nhận sinh viên hiện là trưởng của một đội.
        TeamMember leaderRecord = teamMemberRepository.findByStudentAndIsLeader(userDetails.getAccount().getStudent(), true)
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Leader của đội nào, không thể đăng ký!"));

        // Lấy đội do sinh viên đang đại diện đăng ký sự kiện.
        Team team = leaderRecord.getTeam();
        // Đối chiếu lại mã sinh viên để ngăn dùng bản ghi trưởng nhóm không thuộc tài khoản hiện tại.
        if (leaderRecord.getStudent().getStudentId() != (currentAccount.getStudent().getStudentId())) {
            throw new BadRequestException("Bạn không phải là Leader, bạn không được phép đăng ký event");

        }


        // Kiểm tra đội đã từng gửi đăng ký cho chính sự kiện này hay chưa.
        Optional<Registration> registrationEvent = registrationRepository.findByTeamAndHackathonEvent_EventId(team, event.getEventId());

        // Xử lý theo trạng thái của lần đăng ký đã tồn tại để tránh tạo bản ghi trùng.
        if (registrationEvent.isPresent()) {
            // Lấy đăng ký cũ để kiểm tra hoặc cho phép gửi lại sau khi bị từ chối.
            Registration reg = registrationEvent.get();

            // Đăng ký đang chờ duyệt không được phép gửi thêm lần nữa.
            if (reg.getStatus().equals(RegistrationStatus.PENDING)) {
                throw new BadRequestException("Đội của bạn đã gửi đơn cho sự kiện này rồi. Xin hãy chờ phê duyệt!");
            }
            // Đội đã được duyệt không cần và không được đăng ký lại.
            if (reg.getStatus().equals(RegistrationStatus.APPROVED)) {
                throw new BadRequestException("Đội của bạn đã được phê duyệt cho sự kiện này rồi.");
            }

            // Cho phép đội gửi lại đúng bản ghi cũ nếu lần trước đã bị từ chối.
            if (reg.getStatus().equals(RegistrationStatus.REJECTED)) {
                // Đưa đăng ký cũ về trạng thái chờ xét duyệt.
                reg.setStatus(RegistrationStatus.PENDING);
                // Ghi nhận lại thời điểm đội gửi yêu cầu mới.
                reg.setRegistrationDate(LocalDateTime.now());

                // Lưu thay đổi của đăng ký trước khi cập nhật đội.
                registrationRepository.save(reg);
                // Đánh dấu đội đang chờ kết quả xét duyệt từ ban tổ chức.
                team.setStatus(TeamStatus.PENDING);
                // Lưu trạng thái chờ của đội.
                teamRepository.save(team);

                // Kết thúc vì đã tái sử dụng đăng ký cũ, không tạo thêm bản ghi mới.
                return;
            }
        }
        // Lấy số thành viên chính thức hiện có của đội để so với cấu hình sự kiện.
        int countMember = team.getTeamSize();
        // Từ chối đội chưa đạt số thành viên tối thiểu.
        if (countMember < event.getMinTeamSize()) {
            throw new BadRequestException("Bạn không thể đăng ký cuộc thi. Số lượng thành viên tối thiểu bắt buộc phải lớn hơn hoặc bằng " + event.getMinTeamSize() +
                    " .Thành viên chính thức hiện tại bạn đang sở hữu là " + countMember);
        }
        // Từ chối đội vượt quá số thành viên tối đa.
        if (countMember > event.getMaxTeamSize()) {
            throw new BadRequestException("Bạn không thể đăng ký cuộc thi. Số lượng thành viên tối đa của cuộc thi này là: " + event.getMaxTeamSize() +
                    " .Thành viên chính thức hiện tại bạn đang sở hữu là " + countMember);
        }

        // Trưởng nhóm phải liên kết GitHub trước khi đại diện đội đăng ký.
        if (currentAccount.getGithubId() == null) {
            throw new BadRequestException(
                    "Bạn chưa liên kết tài khoản GitHub. " +
                            "Bạn cần phải tạo tài khoản GitHub trước khi đăng ký tham gia sự kiện.");
        }


        // Khởi tạo đăng ký mới sau khi đội vượt qua toàn bộ điều kiện.
        Registration registration = new Registration();
        // Gắn đăng ký với sự kiện mà đội muốn tham gia.
        registration.setHackathonEvent(event);
        // Gắn đội hiện tại vào đăng ký.
        registration.setTeam(team);
        // Ghi lại thời điểm gửi đăng ký để phục vụ sắp xếp và kiểm tra lịch sử.
        registration.setRegistrationDate(LocalDateTime.now());
        // Đăng ký mới luôn bắt đầu ở trạng thái chờ ban tổ chức xét duyệt.
        registration.setStatus(RegistrationStatus.PENDING);
        // Lưu đăng ký để lấy mã dùng cho lịch sử thao tác.
        registrationRepository.save(registration);

        // Đồng bộ trạng thái đội thành đang chờ duyệt.
        team.setStatus(TeamStatus.PENDING);
        // Lưu trạng thái mới của đội.
        teamRepository.save(team);
        // Ghi lại việc trưởng nhóm đã đăng ký sự kiện thành công.
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
    // Lấy các đội đang chờ ban tổ chức xét duyệt trong sự kiện.
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
    // Lấy toàn bộ đội đã gửi đăng ký để phục vụ việc lựa chọn và quản lý.
    public List<TeamSelectionDTO> getAllTeamRegistrations(Integer evenId) {
        List<Registration> registrations =  registrationRepository.findAll();
        return registrations.stream().map(reg -> new TeamSelectionDTO(reg.getRegistrationId(), reg.getTeam().getTeamName())).toList();
    }


    //Coordinator duyệt Registration — chuyển trạng thái sang APPROVED.
    @Override
    @Transactional
    // Duyệt đăng ký sau khi kiểm tra sức chứa, lịch trùng và trạng thái workshop.
    public Registration approveRegistration(Integer registrationId) {
        // Lấy thông tin người đang xử lý để ghi lịch sử và làm người gửi thông báo.
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Lấy tài khoản ban tổ chức từ thông tin xác thực.
        Account account = userDetails.getAccount();
        // Tìm đăng ký cần duyệt theo mã được gửi lên.
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Registration: " + registrationId));

        // Chỉ đăng ký đang chờ mới có thể chuyển sang trạng thái được duyệt.
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể duyệt Registration ở trạng thái PENDING");
        }

        // Tải sự kiện kèm dữ liệu cần thiết và khóa phù hợp cho quá trình xét duyệt.
        HackathonEvent event = eventRepository
                .findByIdForRegistrationApproval(
                        registration.getHackathonEvent().getEventId()
                )
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy event của Registration"));

        // Không cho phép duyệt sau khi workshop đã bắt đầu hoặc đổi trạng thái.
        validateWorkshopHasNotStarted(event);

        // Đọc giới hạn số đội tối đa được phép tham dự sự kiện.
        Integer maxTeam = event.getMaxTeam();
        // Cấu hình rỗng hoặc nhỏ hơn một không đủ điều kiện để tiến hành xét duyệt.
        if (maxTeam == null || maxTeam < 1) {
            throw new BadRequestException(
                    "Event chưa cấu hình số lượng đội tối đa hợp lệ");
        }

        // Đếm số đội đã được duyệt để tránh vượt quá sức chứa sự kiện.
        long approvedTeamCount =
                registrationRepository.countByHackathonEvent_EventIdAndStatus(
                        event.getEventId(),
                        RegistrationStatus.APPROVED
                );

        // Từ chối yêu cầu khi sự kiện đã nhận đủ số đội theo cấu hình.
        if (approvedTeamCount >= maxTeam) {
            throw new BadRequestException(
                    "Event đã đủ số lượng đội tối đa: " + maxTeam);
        }

        // Lấy đội được gắn với đăng ký đang xử lý.
        Team team = registration.getTeam();
        // Đăng ký thiếu đội là dữ liệu không hợp lệ và không thể tiếp tục duyệt.
        if (team == null) {
            throw new BadRequestException(
                    "Registration không liên kết với team");
        }

        // Kiểm tra đội không đồng thời tham dự một sự kiện khác bị trùng lịch.
        validateNoOverlappingApprovedEvent(
                team,
                registration.getHackathonEvent()
        );

        // Chuyển đăng ký sang trạng thái đã được ban tổ chức chấp thuận.
        registration.setStatus(RegistrationStatus.APPROVED);
        // Lưu ngay thay đổi để các bước sau sử dụng trạng thái mới nhất.
        registration = registrationRepository.saveAndFlush(registration);

        // Đánh dấu đội đang bận vì đã chính thức tham gia sự kiện.
        team.setStatus(TeamStatus.BUSY);
        // Lưu trạng thái thi đấu của đội.
        teamRepository.save(team);

        // Tạo lần tham gia ban đầu để đội có thể được phân vào vòng thi.
        participantService.saveParticipant(registration);
        // Ghi lịch sử duyệt đăng ký cùng người thực hiện và đội được duyệt.
        auditService.saveLog(
                account,
                AuditAction.APPROVE_REGISTRATION,
                AuditEntityType.REGISTRATION,registrationId,
                "Approve registration of team:  " + registration.getTeam().getTeamName()
        );
        // Tìm trưởng nhóm trong danh sách thành viên để nhận thông báo kết quả.
        TeamMember leader = registration.getTeam()
                .getTeamMembers()
                .stream()
                .filter(TeamMember::getIsLeader)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy team leader"));
        // Lấy tài khoản của trưởng nhóm làm người nhận thông báo.
        Account leaderAccount = leader.getStudent().getAccount();
        // Thông báo cho trưởng nhóm rằng đăng ký của đội đã được duyệt.
        notificationService.notifyRegistrationApproved(account, leaderAccount, registration.getTeam().getTeamName(), registration.getHackathonEvent().getEventName());
        // Trả về đăng ký đã cập nhật cho phía gọi.
        return registration;
    }

    // Không cho phép thay đổi đăng ký khi workshop của sự kiện đã bắt đầu.
    private void validateWorkshopHasNotStarted(HackathonEvent event) {
        // Không thể xác định giới hạn xét duyệt nếu sự kiện chưa cấu hình thời gian workshop.
        if (event.getWorkshopTime() == null) {
            throw new BadRequestException(
                    "Event chưa cấu hình thời gian workshop"
            );
        }

        // Workshop phải còn sắp diễn ra và thời gian hiện tại phải nằm trước giờ bắt đầu.
        if (event.getWorkshopStatus() != WorkshopStatus.UPCOMING
                || !LocalDateTime.now().isBefore(event.getWorkshopTime())) {
            throw new BadRequestException(
                    "Chỉ có thể duyệt registration trước khi workshop bắt đầu"
            );
        }
    }

    // Kiểm tra thành viên không tham gia một sự kiện đã duyệt có lịch bị trùng.
    private void validateNoOverlappingApprovedEvent(
            Team team,
            HackathonEvent targetEvent
    ) {
        // Sự kiện đích phải có đầy đủ hai mốc thời gian để thực hiện phép so sánh lịch.
        if (targetEvent == null
                || targetEvent.getStartDate() == null
                || targetEvent.getEndDate() == null) {
            throw new BadRequestException(
                    "Event chưa cấu hình đầy đủ thời gian bắt đầu và kết thúc");
        }

        // Lấy các sự kiện khác mà đội đã được chấp thuận tham gia.
        List<Registration> approvedRegistrations =
                registrationRepository.findByTeam_TeamIdAndStatus(
                        team.getTeamId(),
                        RegistrationStatus.APPROVED
                );

        // Hai khoảng thời gian bị xem là trùng khi mỗi khoảng bắt đầu trước lúc khoảng kia kết thúc.
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

        // Không duyệt thêm nếu đội đã có một sự kiện được duyệt trùng thời gian.
        if (hasOverlap) {
            throw new BadRequestException(
                    "Team đã được duyệt tham gia một event khác trùng thời gian");
        }
    }

    //Coordinator từ chối Registration - Chuyển trạng thái sang REJECTED
    @Override
    @Transactional
    // Từ chối đăng ký và lưu lý do để đội có thể biết nguyên nhân.
    public Registration rejectRegistration(Integer registrationId, String reason) {
        // Lấy người đang xử lý để ghi lịch sử và gửi thông báo từ đúng tài khoản.
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        // Tìm đăng ký cần từ chối và báo lỗi nếu mã không tồn tại.
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Registration: " + registrationId));

        // Chỉ đăng ký đang chờ mới được phép chuyển sang bị từ chối.
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể từ chối Registration ở trạng thái PENDING");
        }

        // Cập nhật kết quả xét duyệt thành bị từ chối.
        registration.setStatus(RegistrationStatus.REJECTED);
        // Lưu trạng thái mới trước khi ghi lịch sử và gửi thông báo.
        registration = registrationRepository.save(registration);
        // Ghi lại quyết định từ chối cùng người thực hiện.
        auditService.saveLog(
                account,
                AuditAction.REJECT_REGISTRATION,
                AuditEntityType.REGISTRATION,registrationId,
                "Reject registration of team:  " + registration.getTeam().getTeamName()
        );
        // Tìm trưởng nhóm để gửi kết quả từ chối đến đúng người đại diện đội.
        TeamMember leader = registration.getTeam()
                .getTeamMembers()
                .stream()
                .filter(TeamMember::getIsLeader)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy team leader"));
        // Lấy tài khoản nhận thông báo từ hồ sơ sinh viên của trưởng nhóm.
        Account leaderAccount = leader.getStudent().getAccount();
        // Gửi thông báo từ chối kèm tên sự kiện và lý do cụ thể.
        notificationService.notifyRegistrationRejected(account, leaderAccount, registration.getTeam().getTeamName(), registration.getHackathonEvent().getEventName(), reason);
        return registration;
    }

    @Override
    // Lấy các đội đã được duyệt tham gia sự kiện.
    public List<TeamSelectionDTO> getApprovedRegistrations(Integer eventId) {
        //1. Lấy danh sách registration đã approve
        List<Registration> registrations =  registrationRepository.findByHackathonEvent_EventIdAndStatus(eventId, RegistrationStatus.APPROVED);
        return registrations.stream().map(reg -> new TeamSelectionDTO(reg.getRegistrationId(), reg.getTeam().getTeamName())).toList();
    }

    @Override
    // Thống kê số lượng đăng ký theo từng trạng thái trong sự kiện.
    public CountRegistrationDTO getCountRegistrations(Integer eventId) {
        // Đếm các đăng ký đã được ban tổ chức chấp thuận.
        Integer countApproved = registrationRepository.countRegistration(RegistrationStatus.APPROVED, eventId);

        // Đếm các đăng ký đã bị từ chối.
        Integer countRejected = registrationRepository.countRegistration(RegistrationStatus.REJECTED, eventId);

        // Đếm các đăng ký vẫn đang chờ xử lý.
        Integer countPending = registrationRepository.countRegistration(RegistrationStatus.PENDING, eventId);

        return CountRegistrationDTO.builder().countApproved(countApproved).countReject(countRejected).countPending(countPending).build();
    }


    @Override
    // Lấy các đăng ký cần chuyển trạng thái khi sự kiện bị hủy.
    public List<Registration> getRegistrationsToCancelled(Integer eventId) {
        // Chỉ đăng ký đang chờ hoặc đã duyệt mới cần xử lý khi sự kiện bị hủy.
        List<RegistrationStatus> list = List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);
        // Tìm các đăng ký thuộc đúng sự kiện và nằm trong nhóm trạng thái cần thay đổi.
        List<Registration> registrations =  registrationRepository.findRegistrationByHackathonEvent_EventIdAndStatusIn(eventId, list);
        return registrations;
    }

    @Override
    // Chuyển danh sách đăng ký sang trạng thái bị từ chối để luồng gọi lưu lại sau đó.
    public void transferStatusToRejectd(List<Registration> registrations) {
        // Duyệt từng đăng ký bị ảnh hưởng bởi việc hủy sự kiện.
        for(Registration registration: registrations){
            // Đánh dấu đăng ký không còn được chấp nhận tham gia.
            registration.setStatus(RegistrationStatus.REJECTED);
        }
    }


    @Override
    // Lấy chi tiết đội và thành viên để ban tổ chức xem trước khi xét duyệt.
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
