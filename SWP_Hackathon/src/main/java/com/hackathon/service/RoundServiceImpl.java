package com.hackathon.service;

import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.validator.RoundValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoundServiceImpl implements RoundService{

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private HackathonEventRepository eventRepository;

    @Autowired
    private CriteriaDetailRepository criteriaDetailRepository;

    @Autowired
    private EvaluationCriteriaService evaluationCriteriaService;

    @Autowired
    private RoundValidator roundValidator;

    @Override
    public Round createRound(CreateRoundRequest request) throws BadRequestException {

        // 1. Validation
        roundValidator.validatorCreate(request);

        //2. Get hackathon event & criteria set
        HackathonEvent event = eventRepository.findById(request.getEventID()).orElseThrow(() -> new RuntimeException("Not found event with ID: " + request.getEventID()));

//        CriteriaSet criteriaSet = criteriaSetRepository.findById(request.getCriteriaSetId()).orElseThrow(() -> new RuntimeException("Not found criteria set with ID: " + request.getCriteriaSetId()));

        //3. Create round
        Round round = new Round();
        round.setRoundName(request.getRoundName());
        round.setStartTime(request.getStartDate());
        round.setEndTime(request.getEndDate());
        round.setAdvancementRule(request.getAdvancementRule());
        round.setOrderIndex(request.getOrderIndex());
        round.setHackathonEvent(event);

        //4. Save DB
        Round savedRound = roundRepository.save(round);

        //5. Custom criteria
        if (request.getCustomCriteriaDetatils() != null && !request.getCustomCriteriaDetatils().isEmpty()) {
            for (EvaluationCriteriaRequestDTO customCriteria : request.getCustomCriteriaDetatils()) {
                evaluationCriteriaService.createEvaluationCritera(customCriteria, request.getCriteriaSetId(), savedRound);
            }

        }
        return savedRound;
    }

    @Override
    public RoundResponse mapToResponse(Round round, List<String> appliedCategoryName) {

        if(round == null){
            return null;
        }

        //1. Map danh sách tiêu chí chấm điểm từ entity sang Response
        List<EvaluationCriteriaResponseDTO> criteriaResponses = new ArrayList<>();
        if(round.getEvaluationCriterias() != null){
            criteriaResponses = round.getEvaluationCriterias().stream().map(evaluationCriteriaService ::mapToResponse).collect(Collectors.toList());
        }
        // lay id event
        Integer eventId = (round.getHackathonEvent() != null) ? round.getHackathonEvent().getEventId() : null;

        return RoundResponse.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .advancementRule(round.getAdvancementRule())
                .endDate(round.getEndTime())
                .startDate(round.getStartTime())
                .eventID(eventId)
                .appliedListCategoryNames(appliedCategoryName)
                .orderIndex(round.getOrderIndex())
                .customCriteriaDetatils(criteriaResponses).build();
    }
}
