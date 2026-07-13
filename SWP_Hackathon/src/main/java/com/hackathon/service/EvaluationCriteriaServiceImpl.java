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
import java.util.stream.Collectors;

//Class nay duoc dung de luu lai tieu chi cham diem da duoc chinh sua or custom tu tieu chi mau

@Service
@RequiredArgsConstructor

public class EvaluationCriteriaServiceImpl implements EvaluationCriteriaService {

    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    @Override
    public EvaluationCriteria createEvaluationCritera(EvaluationCriteriaRequestDTO request, int criteriaSetId, Round round) {

        //tạo EvaluationCriteria để snapshot dữ liệu()
        EvaluationCriteria evaluationCriteria = new EvaluationCriteria();
        evaluationCriteria.setRound(round);
        evaluationCriteria.setCriteriaName(request.getCriteriaName());
        //custom
        evaluationCriteria.setWeight(request.getCustomWeight());
        evaluationCriteria.setDescription(request.getDescription());
        evaluationCriteria.setType(request.getType());

        //lưu xuống DB
        EvaluationCriteria saveEvaluationCriteria = evaluationCriteriaRepository.save(evaluationCriteria);

        return saveEvaluationCriteria;

    }

    @Override
    public EvaluationCriteriaResponseDTO mapToResponse(EvaluationCriteria evaluationCriteria) {
        return EvaluationCriteriaResponseDTO.builder()
                .evaluationCriteriaId(evaluationCriteria.getEvaluationCriteriaId())
                .customWeight(evaluationCriteria.getWeight())
                .criteriaDetailName(evaluationCriteria.getCriteriaName())
                .type(evaluationCriteria.getType())
                .description(evaluationCriteria.getDescription())
                .build();
    }

    @Override
    public List<EvaluationCriteriaResponseDTO> getEvaluationCirteriaResponse(Round round) {

        if(round == null){
            return new ArrayList<>();
        }
        //1. Lấy dữ liệu từ Repository theo id của round
        List<EvaluationCriteria> evaluationCriterias = evaluationCriteriaRepository.findByRound_RoundId(round.getRoundId());
        //2. Nếu dưới db không có dữ liệu thì trả về mảng rỗng
        if(evaluationCriterias == null || evaluationCriterias.isEmpty()){
            return new ArrayList<>();
        }

        //3. Sử dụng Stream API để map taonf bộ danh sách entity sang response
        return evaluationCriterias.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public void deletedEvaluationCriteria(Integer roundId) {
        evaluationCriteriaRepository.deleteByRound_RoundId(roundId);
    }


}