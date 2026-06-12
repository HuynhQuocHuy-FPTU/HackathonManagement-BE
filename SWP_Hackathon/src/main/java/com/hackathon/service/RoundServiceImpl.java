package com.hackathon.service;

import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.validator.RoundValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private ExpertAssignService expertAssignService;

    @Autowired
    private ExpertAssignRepository expertAssignRepository;

    @Autowired
    private CriteriaSetRepository criteriaSetRepository;

    @Override
    public Round createRound(CreateRoundRequest request, int eventId) throws BadRequestException {

        // 1. Validation
        roundValidator.validatorCreate(request);

        //2. Get hackathon event & criteria set
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Not found event with ID: " + eventId));

        CriteriaSet criteriaSet = criteriaSetRepository.findById(request.getCriteriaSetId()).orElseThrow(() -> new BadRequestException("Không tìm thấy criteria set ") );
        //3. Create round
        Round round = new Round();
        round.setRoundName(request.getRoundName());
        round.setStartTime(request.getStartDate());
        round.setEndTime(request.getEndDate());
        round.setAdvancementRule(request.getAdvancementRule());
        round.setStatus(RoundStatus.UPCOMING);
        round.setOrderIndex(request.getOrderIndex());
        round.setCriteriaSet(criteriaSet);
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
    public RoundResponse mapToResponse(Round round) {

        if(round == null){
            return null;
        }

        //1. Map danh sách tiêu chí chấm điểm từ entity sang Response
        List<EvaluationCriteriaResponseDTO> criteriaResponses = new ArrayList<>();
        if(round.getEvaluationCriterias() != null){
            criteriaResponses = round.getEvaluationCriterias().stream().map(evaluationCriteriaService ::mapToResponse).collect(Collectors.toList());
        }

        //2. map danh sách expert sang response
        List<CategoryExpertAssignResponseDTO> expertResponse = expertAssignService.getExpertAssignmentsByRound(round);

        return new RoundResponse(round, criteriaResponses, expertResponse);
    }

}
