package com.hackathon.service;

import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class StatusSchedulerService {

    private final RoundService roundService;
    private final HackathonEventRepository eventRepository;
    @Scheduled(fixedRate = 60000)
    public void updateEventStatusAuto(){
        List<HackathonEvent> events = eventRepository.findByStatus(EventStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        for(HackathonEvent event : events){
            EventStatus newStatus = resolveEventStatus(event, now);

            if(newStatus != null && event.getStatus() != newStatus){
                event.setUpdateAt(LocalDateTime.now());
                event.setStatus(newStatus);
                eventRepository.save(event);
            }
        }
    }
    @Scheduled(fixedRate = 60000)
    public void updateRoundStatusAuto(){
        List<Round> rounds = roundService.getRoundByStatusNot(RoundStatus.COMPLETED);
        LocalDateTime now = LocalDateTime.now();

        for(Round round : rounds){
            RoundStatus newsStatus = this.resolveRoundStatus(round, now);

            if(newsStatus != round.getStatus()){
                round.setStatus(newsStatus);
                roundService.saveRound(round);
            }
        }
    }

    private EventStatus resolveEventStatus(HackathonEvent event, LocalDateTime now){

        EventStatus currentStatus = event.getStatus();

        if(currentStatus == EventStatus.DRAFT || currentStatus == EventStatus.DELETED){
            return null;
        }

        if(currentStatus == EventStatus.ACTIVE && !now.isBefore(event.getRegistrationDeadline())){
            return EventStatus.REGISTRATION_CLOSED;
        }

        if(currentStatus == EventStatus.REGISTRATION_CLOSED && !now.isBefore(event.getStartDate())){
            return EventStatus.ONGOING;
        }

        if(currentStatus == EventStatus.ONGOING && !now.isBefore(event.getEndDate())){
            return EventStatus.COMPLETED;
        }
        return null;
    }

    private RoundStatus resolveRoundStatus(Round round, LocalDateTime now){

        RoundStatus currentStatus = round.getStatus();

        if(now.isBefore(round.getStartTime())){
            return RoundStatus.UPCOMING;
        }

        if(now.isBefore(round.getSubmissionDeadline())){
            return RoundStatus.ONGOING;
        }

        if(now.isBefore(round.getEndTime())){
            return RoundStatus.EVALUATING;
        }

        return RoundStatus.COMPLETED;
    }

}
