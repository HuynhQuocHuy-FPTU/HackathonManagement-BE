package com.hackathon.service;

import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.entity.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.validator.RoundValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service

public class RoundServiceImpl implements RoundService{

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private HackathonEventRepository eventRepository;

//    @Autowired
//    private CriteriaSetRepository criteriaSetRepository;
    @Autowired
    private CriteriaDetailRepository criteriaDetailRepository;

    @Autowired
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

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
        round.setHackathonEvent(event);

        //4. Save DB
        Round savedRound = roundRepository.save(round);

        //5. Custom criteria
        if (request.getCustomCriteriaDetatils() != null && !request.getCustomCriteriaDetatils().isEmpty()) {
            for (EvaluationCriteriaRequestDTO customCriteria : request.getCustomCriteriaDetatils()) {
                // tìm thoong tin tieu chi goc
                CriteriaDetail tempCriteriaDetail = criteriaDetailRepository.findById(customCriteria.getCriteriaDetailId()).orElseThrow(() -> new RuntimeException("Criteria detail not valid with ID: " + customCriteria.getCriteriaDetailId()));

                // kiểm tra tiêu chí con có thuộc bộ tieu chsi không
                if (tempCriteriaDetail.getCriteriaSet().getCriteriaSetId() != request.getCriteriaSetId()) {
                    throw new RuntimeException("Criteria detail not criteria set");
                }

                //tạo CriteriaRound để snapshot dữ liệu()
                EvaluationCriteria evaluationCriteria = new EvaluationCriteria();
                evaluationCriteria.setRound(savedRound);
                evaluationCriteria.setCriteriaDetail(tempCriteriaDetail);
                evaluationCriteria.setCriteriaName(tempCriteriaDetail.getCriteriaName());

                //quyết định có custom hay không
                evaluationCriteria.setWeight(BigDecimal.valueOf(customCriteria.getCustomWeight()));

                //lưu xuống DB
                evaluationCriteriaRepository.save(evaluationCriteria);

            }

        }
        return savedRound;
    }
}
