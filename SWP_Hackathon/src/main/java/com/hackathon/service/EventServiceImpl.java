package com.hackathon.service;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.event.UpdateEventRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.validator.EventValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService{


    private final HackathonEventRepository eventRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EventValidator eventValidator;
    private final CategoryService categoryService;
    private final RoundService roundService;
    private final CategoryRoundRepository categoryRoundRepository;
    private final RoundRepository roundRepository;
    private final CategoryRepository categoryRepository;
    private final EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) throws BadRequestException {

        //1. Validator business rule
        eventValidator.validatorCreate(request);

        //2. Get current user(coordinator)
        //public <X extends Throwable> T orElseThrow(
        //        Supplier<? extends X> exceptionSupplier)
        EventCoordinator coordinator = eventCoordinatorRepository.findById(1).orElseThrow(() -> new BadRequestException("Coodinator not found"));

        //3. Create hackathon event
        HackathonEvent event = new HackathonEvent();
        event.setEventName(request.getEventName());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setSeason(this.generateSeason(request.getStartDate()));
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

        //4. Save DB
        HackathonEvent savedEvent = eventRepository.save(event);

        //5. create categories và save vào map để dễ tra cứu
        Map<String, Category> categoryMap = new HashMap<>();
        if(request.getCategories() != null && !request.getCategories().isEmpty()){
            for(var catRequest : request.getCategories()){

                Category savedCate = categoryService.createCategory(catRequest, savedEvent.getEventId());
                //key: category name, value: category
                categoryMap.put(savedCate.getCategoryName(), savedCate);
            }
        }

        //6. create rounds and categoryRounds
        List<RoundResponse> roundResponses = new ArrayList<>();
        if(request.getRounds() != null && !request.getRounds().isEmpty()){
            for(var roundRequest : request.getRounds()){

                Round saveRound = roundService.createRound(roundRequest, savedEvent.getEventId());

                List<String> selectCategories = roundRequest.getAppliedListCategoryNames();

                if(selectCategories != null && !selectCategories.isEmpty()){
                    for(String cateName : selectCategories){

                        Category matchedCategory = categoryMap.get(cateName);

                        if(matchedCategory != null){
                            CategoryRound roundCategory = new CategoryRound();
                            roundCategory.setRound(saveRound);
                            roundCategory.setCategory(matchedCategory);
                            categoryRoundRepository.save(roundCategory);
                        }
                    }
                }

                RoundResponse roundResponse = roundService.mapToResponse(saveRound, selectCategories);
                roundResponses.add(roundResponse);

            }
        }
        // 7. Chuyển category sang Response
        List<CategoryResponse> categoryResponses = new ArrayList<>();
        if(!categoryMap.isEmpty()){
            categoryResponses = categoryMap.values().stream().map(categoryService :: mapToResponse).collect(Collectors.toList());
        }

        // 8. Chyển event thành response

        return new EventResponse(event, categoryResponses, roundResponses);


    }

    @Override
    public void publishEvent(Integer eventID) {
        HackathonEvent event = eventRepository.findById(eventID).orElseThrow(() -> new BadRequestException("Not found event"));

        //check xem có là draft ko
        if(event.getStatus() != EventStatus.DRAFT){
                throw new BadRequestException("Sự kiện này đã được công bố");
        }
        //đổi trạng thái
        event.setStatus(EventStatus.ACTIVE);
        //lưu lại vào DB
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void updateEvent(UpdateEventRequest request) {
        // lấy event cần update
        HackathonEvent event = eventRepository.findById(request.getEventID()).orElseThrow(() -> new BadRequestException("Not found event by id"));

        //chỉ update nếu là draft
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Sự kiện đã được công bố");
        }

        // Cập nhật thông tin cơ bản
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setSeason(this.generateSeason(request.getStartDate()));
        event.setTitle(request.getTitle());
        event.setAddress(request.getAddress());
        event.setDescription(request.getDescription());
        event.setMaxTeam(request.getMaxTeam());
        event.setMaxTeamSize(request.getMaxTeamSize());
        event.setMinTeamSize(request.getMinTeamSize());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setUpdateAt(LocalDateTime.now());

        HackathonEvent updateEvent = eventRepository.save(event);

        //xóa cấu hình cũ
        //xóa category_round
        categoryRoundRepository.deleteCategoryRoundsByRound_HackathonEvent_EventId(event.getEventId());
        //xóa round
        roundRepository.deleteRoundByHackathonEvent_EventId(event.getEventId());
        //xóa category
        categoryRepository.deleteCategoriesByHackathonEvent_EventId(event.getEventId());

        //5. create categories và save vào map để dễ tra cứu
        Map<String, Category> categoryMap = new HashMap<>();
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (var catRequest : request.getCategories()) {
                Category savedCate = categoryService.createCategory(catRequest, event.getEventId());
                //key: category name, value: category
                categoryMap.put(savedCate.getCategoryName(), savedCate);
            }
        }

        //6. create rounds and categoryRounds
        if (request.getRounds() != null && !request.getRounds().isEmpty()) {
            for (var roundRequest : request.getRounds()) {

                Round saveRound = roundService.createRound(roundRequest, event.getEventId());

                List<String> selectCategories = roundRequest.getAppliedListCategoryNames();

                List<CategoryRound> listCategoryRound = new ArrayList<>();
                if (selectCategories != null && !selectCategories.isEmpty()) {
                    for (String cateName : selectCategories) {

                        Category matchedCategory = categoryMap.get(cateName);

                        if (matchedCategory != null) {
                            CategoryRound roundCategory = new CategoryRound();
                            roundCategory.setRound(saveRound);
                            roundCategory.setCategory(matchedCategory);
                            listCategoryRound.add(roundCategory);
                        }
                    }
                }
                if(!listCategoryRound.isEmpty()){
                    categoryRoundRepository.saveAll(listCategoryRound);
                }

            }


        }
    }

    @Override
    public void deleteEvent(Integer eventID) {
        HackathonEvent event = eventRepository.findById(eventID).orElseThrow(() -> new BadRequestException("Not found event"));

        //check xem có là draft ko
        if(event.getStatus() != EventStatus.DRAFT){
            throw new BadRequestException("Sự kiện này đã được công bố");
        }

        event.setStatus(EventStatus.DELETED);
        event.setUpdateAt(LocalDateTime.now());
        // lưu xuống database
        eventRepository.save(event);

    }

    @Override
    public List<EventResponse> getDeletedEvents() {
        List<HackathonEvent> deledtedEvents = eventRepository.findByStatus(EventStatus.DELETED);

        return deledtedEvents.stream().map(event -> new EventResponse(event, new ArrayList<>(), new ArrayList<>())).collect(Collectors.toList());
    }

    @Override
    public void restoreEvent(Integer eventId) {
        //1.
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        //2. chỉ khôi phục event có trạng thái là deleted
        if(event.getStatus() != EventStatus.DELETED){
            throw new BadRequestException("Sự kiện này không nằm trong thùng rác");
        }

        //3. Đổi trạng thái
        event.setStatus(EventStatus.DRAFT);
        event.setUpdateAt(LocalDateTime.now());

        //4. Lưu xuống DB
        eventRepository.save(event);

    }

    @Override
    public void permanentlyDeleteEvent(Integer eventId) {
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new BadRequestException("Not found event"));

        //check xem có là draft ko
        if(event.getStatus() != EventStatus.DELETED){
            throw new BadRequestException("Sự kiện này chưa thêm vào thùng rác");
        }
        //xóa cấu hình cũ
        //xóa evaluation criteria
        evaluationCriteriaRepository.deleteEvaluationCriteriaByRound_HackathonEvent_EventId(event.getEventId());
        //xóa category_round
        categoryRoundRepository.deleteCategoryRoundsByRound_HackathonEvent_EventId(event.getEventId());
        //xóa round
        roundRepository.deleteRoundByHackathonEvent_EventId(event.getEventId());
        //xóa category
        categoryRepository.deleteCategoriesByHackathonEvent_EventId(event.getEventId());
        //xóa event
        eventRepository.deleteHackathonEventByEventId(event.getEventId());
    }


    // create season
    private String generateSeason(LocalDateTime startDate){
        int year = startDate.getYear();
        int month = startDate.getMonthValue();

        if(month >= 1 && month <= 3){
            return "SPRING " + year;
        }else if(month <= 6){
            return "SUMMER " + year;
        }else if(month <= 9){
            return "FALL " + year;
        }else {
            return "WINTER " + year;
        }

    }
}
