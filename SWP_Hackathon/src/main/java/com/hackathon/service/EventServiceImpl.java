package com.hackathon.service;

import com.hackathon.dto.event.CreateEventRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.validator.EventValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService{

    private final HackathonEventRepository eventRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EventValidator eventValidator;
    private final CategoryService categoryService;
    private final RoundService roundService;
    private final CategoryRoundRepository categoryRoundRepository;
    @Override
    public void createEvent(CreateEventRequest request) throws BadRequestException {

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
               catRequest.setEventId(savedEvent.getEventId());

                Category savedCate = categoryService.createCategory(catRequest);
                //key: category name, value: category
                categoryMap.put(savedCate.getCategoryName(), savedCate);
            }
        }

        //6. create rounds and categoryRounds
        if(request.getRounds() != null && !request.getRounds().isEmpty()){
            for(var roundRequest : request.getRounds()){
                roundRequest.setEventID(savedEvent.getEventId());

                Round saveRound = roundService.createRound(roundRequest);

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

            }
        }
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
