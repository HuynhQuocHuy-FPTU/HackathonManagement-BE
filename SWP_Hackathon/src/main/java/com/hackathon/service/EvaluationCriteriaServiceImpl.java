package com.hackathon.service;

import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.entity.CriteriaDetail;
import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.CriteriaDetailRepository;
import com.hackathon.repository.EvaluationCriteriaRepository;
import com.hackathon.repository.RoundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.entity.Round;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

//Class nay duoc dung de luu lai tieu chi cham diem da duoc chinh sua or custom tu tieu chi mau

@Service
@RequiredArgsConstructor

public class EvaluationCriteriaServiceImpl implements EvaluationCriteriaService {

    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    private final RoundRepository roundRepository;
    private final CriteriaDetailRepository criteriaDetailRepository;

    @Override
    public EvaluationCriteria createEvaluationCritera(EvaluationCriteriaRequestDTO request, int criteriaSetId, Round round) {
        //lay ra tieu chi goc
        CriteriaDetail tempCriteriaDetail = criteriaDetailRepository.findById(request.getCriteriaDetailId()).orElseThrow(() -> new RuntimeException("Criteria detail not valid with ID: " + request.getCriteriaDetailId()));

        // kiểm tra tiêu chí con có thuộc bộ tieeu chi không
        if (tempCriteriaDetail.getCriteriaSet().getCriteriaSetId() != criteriaSetId) {
            throw new RuntimeException("Criteria detail not criteria set");
        }

        //tạo EvaluationCriteria để snapshot dữ liệu()
        EvaluationCriteria evaluationCriteria = new EvaluationCriteria();
        evaluationCriteria.setRound(round);
        evaluationCriteria.setCriteriaDetail(tempCriteriaDetail);
        evaluationCriteria.setCriteriaName(tempCriteriaDetail.getCriteriaName());

        //custom
        evaluationCriteria.setWeight(BigDecimal.valueOf(request.getCustomWeight()));

        //lưu xuống DB
        EvaluationCriteria saveEvaluationCriteria = evaluationCriteriaRepository.save(evaluationCriteria);

        return saveEvaluationCriteria;

    }

    @Override
    public EvaluationCriteriaResponseDTO mapToResponse(EvaluationCriteria evaluationCriteria) {
        return EvaluationCriteriaResponseDTO.builder()
                .criteriaDetailId(evaluationCriteria.getEvaluationCriteriaId())
                .customWeight(evaluationCriteria.getEvaluationCriteriaId()).criteriaDetailName(evaluationCriteria.getCriteriaName())
                .description(evaluationCriteria.getDescription())
                .build();
    }
}