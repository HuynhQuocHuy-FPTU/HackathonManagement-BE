package com.hackathon.service.impl;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.dto.event.UpdateTimeEventDTO;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.WorkshopStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.TeamRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.*;
import com.hackathon.service.EventService;
import com.hackathon.validator.EventValidator;
import com.hackathon.validator.RoundValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
// Quản lý toàn bộ vòng đời sự kiện từ lúc tạo, công khai, cập nhật đến khi kết thúc hoặc xóa.
public class EventServiceImpl implements EventService {

    private final HackathonEventRepository eventRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EventValidator eventValidator;
    private final CategoryService categoryService;
    private final RoundService roundService;
    private final CategoryRoundService categoryRoundService;
    private final ExpertAssignService expertAssignService;
    private final RegistrationEventService registrationEventService;
    private final RoundValidator roundValidator;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TeamRepository teamRepository;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    // Kiểm tra dữ liệu đầu vào, tạo sự kiện cùng các vòng thi và danh mục được cấu hình ban đầu.
    public EventResponse createEvent(CreateEventRequest request) throws BadRequestException {

        // 1. Validate business rule trước khi chạm DB
        eventValidator.validatorCreate(request);

        // 2. Lấy thông tin Coordinator từ phiên đăng nhập
        String currentEmail = getCurrentEmail();
        EventCoordinator coordinator = eventCoordinatorRepository.findByAccount_Email(currentEmail)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Quản trị viên: " + currentEmail));

        // 3. Khởi tạo Event mới
        HackathonEvent event = new HackathonEvent();
        if (request.getEventName() != null) event.setEventName(request.getEventName());
        if (request.getStartDate() != null) event.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) event.setEndDate(request.getEndDate());
        event.setSeason(request.getSeason());
        if (request.getStartDate() != null) event.setSeasonYear(request.getStartDate().getYear());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getAddress() != null) event.setAddress(request.getAddress());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getMaxTeam() != null) event.setMaxTeam(request.getMaxTeam());
        if (request.getMinTeam() != null) event.setMinTeam(request.getMinTeam());
        if (request.getMaxTeamSize() != null) event.setMaxTeamSize(request.getMaxTeamSize());
        if (request.getMinTeamSize() != null) event.setMinTeamSize(request.getMinTeamSize());
        if (request.getRegistrationDeadline() != null) event.setRegistrationDeadline(request.getRegistrationDeadline());
        if (request.getWorkshopTime() != null) {
            event.setWorkshopTime(request.getWorkshopTime());
            event.setWorkshopStatus(WorkshopStatus.UPCOMING);
        }
        if (request.getBannerUrl() != null) event.setBannerUrl(request.getBannerUrl());

        event.setEventCoordinator(coordinator);
        event.setCreateAt(LocalDateTime.now());
        event.setStatus(EventStatus.DRAFT);
        log.info("Before save eventId = {}", event.getEventId());

        // 4. Lưu sơ bộ để sinh EventId
        HackathonEvent savedEvent = eventRepository.save(event);

        // 5. Tạo danh sách Category gắn với Event
        List<Category> categories = new ArrayList<>();
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            categories = categoryService.createCategory(request.getCategories(), savedEvent.getEventId());
        }

        // 6. Tạo các Round và thiết lập liên kết trung gian
        List<Round> createdRounds = new ArrayList<>();

        if (request.getRounds() != null && !request.getRounds().isEmpty()) {
            for (var roundRequest : request.getRounds()) {
                Round savedRound = roundService.createRound(roundRequest, savedEvent.getEventId());
                createdRounds.add(savedRound);

                List<CategoryRound> categoryRounds = new ArrayList<>();
                if (!categories.isEmpty()) {
                    categoryRounds = categoryRoundService.createCategoryRound(categories, savedRound);
                }
                expertAssignService.assignExpertsToCategoryRound(categoryRounds, roundRequest.getCategoryExperts(), savedRound);
            }
        }

        // 7. Đồng bộ quan hệ trước khi build Response
        savedEvent.getRounds().clear();
        savedEvent.getRounds().addAll(createdRounds);
        savedEvent = eventRepository.saveAndFlush(savedEvent);
        auditService.saveLog(
                coordinator.getAccount(),
                AuditAction.CREATE_EVENT,
                AuditEntityType.EVENT,
                event.getEventId(),
                "Create event " + event.getEventName()
        );



        return mapToResponse(savedEvent, createdRounds, categories);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    // Cập nhật thông tin sự kiện và đồng bộ lại danh sách vòng thi, danh mục liên quan.
    public EventResponse updateEvent(UpdateEventRequest request, Integer eventId) {

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();

        // 1. Lấy event cần update
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        eventValidator.updateEventValidator(request, event);

        if (request.getRounds() != null && !request.getRounds().isEmpty()) {
            roundValidator.validateRoundsTimelineByOrder(request.getRounds());
        }

        // 2. Cập nhật thông tin cơ bản
        event.setEventName(request.getEventName());
        event.setTitle(request.getTitle());
        event.setAddress(request.getAddress());
        event.setDescription(request.getDescription());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setMaxTeam(request.getMaxTeam());
        event.setMinTeam(request.getMinTeam());
        event.setMaxTeamSize(request.getMaxTeamSize());
        event.setMinTeamSize(request.getMinTeamSize());
        event.setBannerUrl(request.getBannerUrl());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setWorkshopTime(request.getWorkshopTime());
        event.setSeason(request.getSeason());
        if (request.getStartDate() != null) event.setSeasonYear(request.getStartDate().getYear());

        event.setUpdateAt(LocalDateTime.now());

        HackathonEvent updatedEvent = eventRepository.save(event);

        // 3. Xóa cấu hình cũ các bảng trung gian (qua Service)
        expertAssignService.deleteByEventId(eventId);
        categoryRoundService.deleteByEventId(eventId);

        // 4. Xử lý Categories (thêm / sửa / xóa)
        List<Category> freshCategories = categoryService.updateCategories(request.getCategories(), updatedEvent);
        if (freshCategories != null) {
            for (Category cat : freshCategories) {
                cat.setHackathonEvent(updatedEvent); // <--- BẮT BUỘC: Gán event cho category

            }
            updatedEvent.setCategories(freshCategories); // Thêm vào list của event
        }
        updatedEvent = eventRepository.saveAndFlush(updatedEvent);
// Dùng trực tiếp freshCategories — đáng tin hơn updatedEvent.getCategories()
// vì đó là LAZY collection, có thể không phản ánh đúng state ngay sau saveAndFlush
        List<Category> finalCategories = freshCategories != null ? freshCategories : new ArrayList<>();

        // 5. Xử lý Rounds và các liên kết trung gian
        List<Round> updatedRounds = new ArrayList<>();

        log.info(">>> [DEBUG] finalCategories size = {}", finalCategories.size());
        finalCategories.forEach(c -> log.info(">>> [DEBUG] category id={}, name={}", c.getCategoryId(), c.getCategoryName()));

        if (request.getRounds() != null && !request.getRounds().isEmpty()) {

            // Lấy 1 lần trước vòng lặp — tránh N+1 query
            List<Round> currentRounds = roundService.findAllByEventId(eventId);
            roundService.deleteRoundsExcluding(request.getRounds(), currentRounds);

            for (var roundRequest : request.getRounds()) {
                Round savedRound = roundService.updateSingleRound(roundRequest, currentRounds, eventId);

                log.info(">>> [DEBUG] processing round id={}", savedRound.getRoundId());

                List<CategoryRound> categoryRounds = new ArrayList<>();
                if (!finalCategories.isEmpty()) {
                    categoryRounds = categoryRoundService.createCategoryRound(finalCategories, savedRound);
                }

                if (roundRequest.getCategoryExperts() != null && !roundRequest.getCategoryExperts().isEmpty()) {
                    expertAssignService.assignExpertsToCategoryRound(
                            categoryRounds,
                            roundRequest.getCategoryExperts(),
                            savedRound
                    );
                }
                updatedRounds.add(savedRound);
            }
            updatedEvent.getRounds().clear();
            updatedEvent.getRounds().addAll(updatedRounds);
            updatedEvent = eventRepository.save(updatedEvent);

        } else {
            // Rounds rỗng → xóa sạch tất cả round của event
            roundService.deleteByEventId(eventId);
        }
        auditService.saveLog(
                account,
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                event.getEventId(),
                "Updated event " + event.getEventName()
        );

        return mapToResponse(updatedEvent, updatedRounds, finalCategories);
    }

    // =========================================================
    // PUBLISH
    // =========================================================

    @Override
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    // Công khai sự kiện sau khi xác nhận sự kiện đã có đủ dữ liệu bắt buộc.
    public void publishEvent(Integer eventId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        eventValidator.publishEventValidator(event);

        event.setStatus(EventStatus.ACTIVE);
        eventRepository.save(event);
        auditService.saveLog(
                account,
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                event.getEventId(),
                "Publish event " + event.getEventName()
        );
    }

    // =========================================================
    // DELETE (soft)
    // =========================================================

    @Override
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    // Đánh dấu sự kiện đã xóa để ẩn khỏi các danh sách hoạt động nhưng vẫn có thể khôi phục.
    public void deleteEvent(Integer eventId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể xóa sự kiện ở trạng thái DRAFT");
        }

        event.setStatus(EventStatus.DELETED);
        event.setUpdateAt(LocalDateTime.now());
        eventRepository.save(event);
        auditService.saveLog(
                account,
                AuditAction.DELETE_EVENT,
                AuditEntityType.EVENT,
                event.getEventId(),
                "Publish event " + event.getEventName()
        );
    }

    // =========================================================
    // RESTORE
    // =========================================================

    @Override
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    // Khôi phục sự kiện đã xóa mềm và đưa sự kiện trở lại trạng thái quản lý bình thường.
    public void restoreEvent(Integer eventId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        if (event.getStatus() != EventStatus.DELETED) {
            throw new BadRequestException("Sự kiện này không nằm trong thùng rác");
        }

        event.setStatus(EventStatus.DRAFT);
        event.setUpdateAt(LocalDateTime.now());
        eventRepository.save(event);
        auditService.saveLog(
                account,
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                event.getEventId(),
                "Restore event " + event.getEventName()
        );
    }

    // =========================================================
    // CANCELLED
    // =========================================================
    @Override
    @Transactional
    // Hủy sự kiện theo yêu cầu của người có quyền và ghi nhận lý do cùng người thực hiện.
    public void cancelEvent(Integer eventId, String reason, CustomUserDetails currentUser) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy event"));

        // Ràng buộc 1: Kiểm tra quyền
        if (!event.getEventCoordinator().getAccount().equals(currentUser.getAccount())) {
            throw new AccessDeniedException("Bạn không có quyền hủy sự kiện này.");
        }

        // Ràng buộc 2: Trạng thái không được là COMPLETED
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy sự kiện đã kết thúc.");
        }
        if (event.getStatus() == EventStatus.DRAFT) {
            throw new IllegalStateException("Không thể hủy sự kiện chưa được công bố.");
        }

        // Ràng buộc 3: Phải có lý do
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy sự kiện.");
        }

        performCancellation(event, reason, currentUser.getAccount());
    }

    @Override
    @Transactional
    // Hủy sự kiện tự động khi tác vụ nền phát hiện sự kiện không còn đáp ứng điều kiện tiếp tục.
    public void cancelEventAutomatically(Integer eventId, String reason) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        if (event.getStatus() == EventStatus.CANCELLED) {
            return;
        }
        if (event.getStatus() != EventStatus.ACTIVE
                && event.getStatus() != EventStatus.REGISTRATION_CLOSED) {
            throw new BadRequestException("Trạng thái event không cho phép hệ thống tự động hủy");
        }

        Account coordinatorAccount = event.getEventCoordinator().getAccount();
        performCancellation(event, reason, coordinatorAccount);
        notificationService.notifyAutoCancelledEventCoordinator(
                coordinatorAccount,
                event.getEventName(),
                reason
        );
    }

    // Dùng chung quy trình đổi trạng thái, lưu lý do và gửi thông báo khi sự kiện bị hủy.
    private void performCancellation(HackathonEvent event, String reason, Account actor) {
        event.setStatus(EventStatus.CANCELLED);
        event.setCancellationReason(reason);
        event.setUpdateAt(LocalDateTime.now());
        eventRepository.save(event);

        List<Registration> registrationList = registrationEventService
                .getRegistrationsToCancelled(event.getEventId());
        registrationEventService.transferStatusToRejectd(registrationList);

        List<Team> teams = registrationList.stream()
                .map(Registration::getTeam)
                .distinct()
                .peek(team -> team.setStatus(
                        com.hackathon.entity.enums.TeamStatus.ACTIVE))
                .toList();
        teamRepository.saveAll(teams);

        List<Account> accLeaders = registrationList.stream()
                .map(Registration::getTeam)
                .flatMap(team -> team.getTeamMembers().stream())
                .filter(TeamMember::getIsLeader)
                .map(member -> member.getStudent().getAccount())
                .distinct()
                .toList();

        auditService.saveLog(
                actor,
                AuditAction.CANCELLD_EVENT,
                AuditEntityType.EVENT,
                event.getEventId(),
                "Cancelled event: " + event.getEventName()
        );
        notificationService.notifyCancelledEvent(actor, accLeaders, event.getEventName(), reason);
    }

    // =========================================================
    // PERMANENTLY DELETE
    // =========================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    // Xóa vĩnh viễn sự kiện đã xóa mềm cùng các dữ liệu phụ thuộc theo quy tắc hệ thống.
    public void permanentlyDeleteEvent(Integer eventId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Account account = userDetails.getAccount();
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        if (event.getStatus() != EventStatus.DELETED) {
            throw new BadRequestException("Sự kiện chưa được đưa vào thùng rác");
        }

        eventRepository.delete(event);
        auditService.saveLog(
                account,
                AuditAction.UPDATE_EVENT,
                AuditEntityType.EVENT,
                event.getEventId(),
                "Xóa cuộc thi vĩnh viễn" + event.getEventName()
        );
    }

    // =========================================================
    // QUERY
    // =========================================================

    @Override
    // Lấy chi tiết một sự kiện cùng các vòng thi và danh mục để trả về cho người dùng.
    public EventResponse getEventDetail(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        return mapToResponse(event, event.getRounds(), event.getCategories());
    }

    @Override
    // Lấy toàn bộ sự kiện chưa bị xóa để phục vụ màn hình quản lý.
    public List<EventResponse> getAllEvent() {
        return eventRepository.findAll().stream()
                .map(event -> mapToResponse(event, event.getRounds(), new ArrayList<>()))
                .toList();
    }

    @Override
    // Lọc danh sách sự kiện quản lý theo năm tổ chức được yêu cầu.
    public List<EventResponse> getAllEventsByYear(Integer seasonYear) {
        return eventRepository.findBySeasonYear(seasonYear).stream()
                .map(event -> mapToResponse(event, event.getRounds(), new ArrayList<>()))
                .toList();
    }

    @Override
    // Lấy danh sách các năm có sự kiện để tạo bộ lọc trên giao diện quản lý.
    public List<Integer> getAllEventYears() {
        return eventRepository.findDistinctSeasonYears();
    }

    @Override
    // Lấy các sự kiện đã công khai và đủ điều kiện hiển thị cho người dùng bên ngoài.
    public List<EventResponse> getPublicEvents() {
        return eventRepository.findByStatusNotIn(List.of(EventStatus.DRAFT, EventStatus.DELETED, EventStatus.CANCELLED)).stream().map(event -> mapToResponse(event, event.getRounds(), event.getCategories()
                )).toList();
    }

    @Override
    // Lọc các sự kiện công khai theo năm tổ chức.
    public List<EventResponse> getPublicEventsByYear(Integer seasonYear) {
        List<EventStatus> excludedStatuses = List.of(
                EventStatus.DRAFT,
                EventStatus.DELETED,
                EventStatus.CANCELLED
        );
        return eventRepository.findBySeasonYearAndStatusNotIn(seasonYear, excludedStatuses).stream()
                .map(event -> mapToResponse(event, event.getRounds(), event.getCategories()))
                .toList();
    }

    @Override
    // Lấy danh sách năm đang có ít nhất một sự kiện công khai.
    public List<Integer> getPublicEventYears() {
        return eventRepository.findDistinctSeasonYearsByStatusNotIn(List.of(
                EventStatus.DRAFT,
                EventStatus.DELETED,
                EventStatus.CANCELLED
        ));
    }

    @Override
    // Tìm các sự kiện công khai có tên phù hợp với từ khóa người dùng nhập.
    public List<EventResponse> searchPublicEvents(String eventName) {
        return eventRepository.findHackathonEventByEventNameContainingIgnoreCaseAndStatus(eventName, EventStatus.ACTIVE).stream().map(event -> mapToResponse(event, event.getRounds(), event.getCategories())).toList();
    }

    @Override
    // Cập nhật các mốc thời gian của sự kiện sau khi kiểm tra quyền và tính hợp lệ của lịch mới.
    public void updateTimeEvent(CustomUserDetails userDetails, Integer eventId, UpdateTimeEventDTO updateTimeEventDTO) {
        EventCoordinator eventCoordinator = userDetails.getAccount().getEventCoordinator();

        if(eventCoordinator == null){
            throw new BadRequestException("Bạn không có quyền truy cập. Bạn phải là event coordinator");
        }

        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy cuộc thi"));

        eventValidator.validateTimeLogic(
                updateTimeEventDTO.startTime(),
                updateTimeEventDTO.endTime(),
                updateTimeEventDTO.registrationDeadline(),
                updateTimeEventDTO.workshopTime(),
                LocalDateTime.now(),
                false
        );
        eventValidator.validateEventEndAfterRounds(
                updateTimeEventDTO.endTime(),
                event
        );

        event.setStartDate(updateTimeEventDTO.startTime());
        if (updateTimeEventDTO.startTime() != null) {
            event.setSeasonYear(updateTimeEventDTO.startTime().getYear());
        }
        event.setEndDate(updateTimeEventDTO.endTime());
        event.setWorkshopTime(updateTimeEventDTO.workshopTime());
        event.setRegistrationDeadline(updateTimeEventDTO.registrationDeadline());

        eventRepository.save(event);
    }

    @Override
    // Lấy danh sách sự kiện đã xóa mềm để quản trị viên có thể khôi phục hoặc xóa vĩnh viễn.
    public List<EventResponse> getDeletedEvents() {
        return eventRepository.findByStatus(EventStatus.DELETED).stream()
                .map(event -> mapToResponse(event, new ArrayList<>(), new ArrayList<>()))
                .toList();
    }

    @Override
    // Tìm sự kiện trong khu vực quản lý theo tên, không giới hạn ở sự kiện công khai.
    public List<EventResponse> searchByEventName(String eventName) {
        return eventRepository.findByEventNameContainingIgnoreCase(eventName).stream()
                .map(event -> mapToResponse(event, new ArrayList<>(), new ArrayList<>()))
                .toList();
    }


    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    // Lấy thư điện tử của tài khoản đang xác thực để xác định người thực hiện thao tác.
    private String getCurrentEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BadRequestException("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
        }

        return authentication.getName();
    }
    private EventResponse mapToResponse(HackathonEvent event, List<Round> rounds, List<Category> categories){
        List<RoundResponse> roundResponses = rounds.stream().map(roundService::mapToResponse).toList();
        List<CategoryResponse> categoryResponses = categories.stream().map(categoryService::mapToResponse).toList();
        return new EventResponse(event, categoryResponses, roundResponses);

    }

}
