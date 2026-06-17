package com.hackathon.service.event;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.service.CategoryRoundService;
import com.hackathon.service.CategoryService;
import com.hackathon.service.ExpertAssignService;
import com.hackathon.service.RoundService;
import com.hackathon.validator.EventValidator;
import com.hackathon.validator.RoundValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class EventServiceImpl implements EventService {

    private final HackathonEventRepository eventRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EventValidator eventValidator;
    private final CategoryService categoryService;
    private final RoundService roundService;
    private final CategoryRoundService categoryRoundService;
    private final ExpertAssignService expertAssignService;
    private final RoundValidator roundValidator;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public EventResponse createEvent(CreateEventRequest request) throws BadRequestException {

        // 1. Validate business rule trước khi chạm DB
        eventValidator.validatorCreate(request);

        // 2. Lấy thông tin Coordinator từ phiên đăng nhập
        String currentEmail = getCurrentEmail();
        EventCoordinator coordinator = eventCoordinatorRepository.findByAccount_Email(currentEmail)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Quản trị viên: " + currentEmail));

        // 3. Khởi tạo Event mới
        HackathonEvent event = new HackathonEvent();
        event.setEventName(request.getEventName());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setSeason(generateSeason(request.getStartDate()));
        event.setTitle(request.getTitle());
        event.setAddress(request.getAddress());
        event.setDescription(request.getDescription());
        event.setMaxTeam(request.getMaxTeam());
        event.setMaxTeamSize(request.getMaxTeamSize());
        event.setMinTeamSize(request.getMinTeamSize());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setEventCoordinator(coordinator);
        event.setCreateAt(LocalDateTime.now());
        event.setStatus(EventStatus.DRAFT);

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



        return mapToResponse(savedEvent, createdRounds, categories);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public EventResponse updateEvent(UpdateEventRequest request, Integer eventId) {

        // 1. Lấy event cần update
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        eventValidator.updateEventValidator(request, event);

        if (request.getRounds() != null && !request.getRounds().isEmpty()) {
            roundValidator.validateRoundsTimelineByOrder(request.getRounds());
        }

        // 2. Cập nhật thông tin cơ bản
        event.setEventName(request.getEventName());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setSeason(generateSeason(request.getStartDate()));
        event.setTitle(request.getTitle());
        event.setAddress(request.getAddress());
        event.setDescription(request.getDescription());
        event.setMaxTeam(request.getMaxTeam());
        event.setMaxTeamSize(request.getMaxTeamSize());
        event.setMinTeamSize(request.getMinTeamSize());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setUpdateAt(LocalDateTime.now());

        HackathonEvent updatedEvent = eventRepository.save(event);

        // 3. Xóa cấu hình cũ các bảng trung gian (qua Service)
        expertAssignService.deleteByEventId(eventId);
        categoryRoundService.deleteByEventId(eventId);

        // 4. Xử lý Categories (thêm / sửa / xóa)
        List<Category> freshCategories = categoryService.updateCategories(request.getCategories(), updatedEvent);
        updatedEvent.getCategories().clear();
        if (freshCategories != null) {
            updatedEvent.getCategories().addAll(freshCategories);
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
                    log.info(">>> [DEBUG] categoryRounds created = {}", categoryRounds.size());
                    if (categoryRounds.isEmpty()) {
                        throw new BadRequestException("Không tạo được bản ghi CategoryRound cho round: " + savedRound.getRoundId());
                    }
                }

                expertAssignService.assignExpertsToCategoryRound(categoryRounds, roundRequest.getCategoryExperts(), savedRound);
                updatedRounds.add(savedRound);
            }

            updatedEvent.getRounds().clear();
            updatedEvent.getRounds().addAll(updatedRounds);
            updatedEvent = eventRepository.save(updatedEvent);

        } else {
            // Rounds rỗng → xóa sạch tất cả round của event
            roundService.deleteByEventId(eventId);
        }

        return mapToResponse(updatedEvent, updatedRounds, finalCategories);
    }

    // =========================================================
    // PUBLISH
    // =========================================================

    @Override
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public void publishEvent(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        eventValidator.publishEventValidator(event);

        event.setStatus(EventStatus.ACTIVE);
        eventRepository.save(event);
    }

    // =========================================================
    // DELETE (soft)
    // =========================================================

    @Override
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public void deleteEvent(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể xóa sự kiện ở trạng thái DRAFT");
        }

        event.setStatus(EventStatus.DELETED);
        event.setUpdateAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    // =========================================================
    // RESTORE
    // =========================================================

    @Override
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public void restoreEvent(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        if (event.getStatus() != EventStatus.DELETED) {
            throw new BadRequestException("Sự kiện này không nằm trong thùng rác");
        }

        event.setStatus(EventStatus.DRAFT);
        event.setUpdateAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    // =========================================================
    // PERMANENTLY DELETE
    // =========================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public void permanentlyDeleteEvent(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        if (event.getStatus() != EventStatus.DELETED) {
            throw new BadRequestException("Sự kiện chưa được đưa vào thùng rác");
        }

        eventRepository.delete(event);
    }

    // =========================================================
    // QUERY
    // =========================================================

    @Override
    public EventResponse getEventDetail(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sự kiện"));

        return mapToResponse(event, event.getRounds(), event.getCategories());
    }

    @Override
    public List<EventResponse> getAllEvent() {
        return eventRepository.findAll().stream()
                .map(event -> mapToResponse(event, new ArrayList<>(), new ArrayList<>()))
                .toList();
    }

    @Override
    public List<EventResponse> getPublicEvents() {
        return eventRepository.findByStatus(EventStatus.ACTIVE).stream().map(event -> mapToResponse(event, event.getRounds(), event.getCategories()
                )).toList();
    }

    @Override
    public List<EventResponse> searchPublicEvents(String eventName) {
        return eventRepository.findHackathonEventByEventNameContainingIgnoreCaseAndStatus(eventName, EventStatus.ACTIVE).stream().map(event -> mapToResponse(event, event.getRounds(), event.getCategories())).toList();
    }

    @Override
    public List<EventResponse> getDeletedEvents() {
        return eventRepository.findByStatus(EventStatus.DELETED).stream()
                .map(event -> mapToResponse(event, new ArrayList<>(), new ArrayList<>()))
                .toList();
    }

    @Override
    public List<EventResponse> searchByEventName(String eventName) {
        return eventRepository.findByEventNameContainingIgnoreCase(eventName).stream()
                .map(event -> mapToResponse(event, new ArrayList<>(), new ArrayList<>()))
                .toList();
    }


//    @Override
//    public Page<HackathonEvent> getPage(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        return eventRepository.findAll(pageable);
//    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private String generateSeason(LocalDateTime startDate) {
        int year = startDate.getYear();
        int month = startDate.getMonthValue();

        if (month <= 4) return "SPRING " + year;
        if (month <= 8) return "SUMMER " + year;
        return "FALL " + year;
    }

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