package com.hackathon.service;

import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.repository.EvaluationCriteriaRepository;
import com.hackathon.repository.RoundRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hackathon.dto.criteria.CriteriaCustomDTO;

import com.hackathon.entity.Round;

import java.util.ArrayList;
import java.util.List;

//Class nay duoc dung de luu lai tieu chi cham diem da duoc chinh sua or custom tu tieu chi mau

@Service
public class EvaluationCriteriaServiceImpl implements EvaluationCriteriaService {

    @Autowired
    EvaluationCriteriaRepository evaluationCriteriaRepository;
    @Autowired
    private RoundRepository roundRepository;


    @Override
    @Transactional
    public List<EvaluationCriteriaRequestDTO> getEvaluationCriteriaByCriteriaId(Integer criteriaId) {
        List<EvaluationCriteria> evaluationCriteria =
                evaluationCriteriaRepository.findByEvaluationCriteriaId(criteriaId);
        return evaluationCriteria.stream()
                .map(criteria -> {
                    EvaluationCriteriaRequestDTO dto = new EvaluationCriteriaRequestDTO();
                    dto.setEvaluationCriteriaId(criteria.getEvaluationCriteriaId());
                    dto.setCriteriaName(criteria.getCriteriaName());
                    dto.setWeight(criteria.getWeight());
                    dto.setDescription(criteria.getDescription());
                    return dto;
                })
                .toList();

    }


    @Override
    @Transactional
    public EvaluationCriteriaResponseDTO saveEvaluationCriteria(EvaluationCriteriaRequestDTO request) {

        // 1. Find Round
        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new RuntimeException(
                        "No Round With Id: " + request.getRoundId()));

        List<EvaluationCriteria> listSave = new ArrayList<>();

        // 2.
        if (request.getCriteriaList() != null && !request.getCriteriaList().isEmpty()) {

            for (CriteriaCustomDTO dto : request.getCriteriaList()) {

                EvaluationCriteria entity;

                if (dto.getEvaluationCriteriaId() != null) {
                    entity = evaluationCriteriaRepository.findById(dto.getEvaluationCriteriaId())
                            .orElseThrow(() -> new RuntimeException("EvaluationCriteria not found"));
                } else {
                    entity = new EvaluationCriteria();
                }

                entity.setEventId(request.getEventId());
                entity.setRound(round);
                entity.setCriteriaName(dto.getCriteriaName());
                entity.setDescription(dto.getDescription());
                entity.setWeight(dto.getWeight());
                entity.setCriteriaDetailId(dto.getCriteriaDetailId());

                listSave.add(entity);
            }
        }

        // 3. Save DB
        List<EvaluationCriteria> savedList =
                evaluationCriteriaRepository.saveAll(listSave);

        // 4. Build response
        EvaluationCriteriaResponseDTO response = new EvaluationCriteriaResponseDTO();

        response.setEventId(round.getHackathonEvent().getEventId());
        response.setCriteriaSetId(request.getCriteriaSetId());
        response.setEvaluationName(request.getCriteriaName());

        if (!savedList.isEmpty()) {
            response.setEvaluationId(savedList.get(0).getEvaluationCriteriaId());
        }

        // 5. Map items
        List<EvaluationCriteriaResponseDTO.EvaluationItemResponseDTO> itemList =
                new ArrayList<>();

        for (EvaluationCriteria entity : savedList) {

            EvaluationCriteriaResponseDTO.EvaluationItemResponseDTO item =
                    new EvaluationCriteriaResponseDTO.EvaluationItemResponseDTO();

            item.setEvaluationItemId(entity.getEvaluationCriteriaId());
            item.setCriteriaDetailId(entity.getCriteriaDetailId());
            item.setCriteriaName(entity.getCriteriaName());
            item.setDescription(entity.getDescription());
            item.setMaxScore(entity.getWeight());
            item.setCriteriaDetailId(entity.getCriteriaDetailId());
            itemList.add(item); // FIXED
        }

        // 6. Set items
        response.setItems(itemList);
        return response;
    }

    @Override
    public EvaluationCriteriaResponseDTO getByEventId(Integer eventId) {
        return null;
    }

    @Override
    public void updateEvaluationCriteria(EvaluationCriteriaRequestDTO evaluationCriteriaRequestDTO) {
        Round round = roundRepository.findById(evaluationCriteriaRequestDTO.getRoundId()).orElseThrow(() -> new RuntimeException("Round not found"));
        if (evaluationCriteriaRequestDTO.getCriteriaList() == null || evaluationCriteriaRequestDTO.getCriteriaList().isEmpty()) {
            return;
        }
        List<EvaluationCriteria> listToUpdate = new ArrayList<>();
        for (CriteriaCustomDTO dto : evaluationCriteriaRequestDTO.getCriteriaList()) {
            if (dto.getEvaluationCriteriaId() == null) {
                throw new RuntimeException("Missing ID For update");
            }
            EvaluationCriteria evalCriteria = evaluationCriteriaRepository.findById(dto.getEvaluationCriteriaId()).orElseThrow(() -> new RuntimeException("Criteria not found"));
            evalCriteria.setEventId(evaluationCriteriaRequestDTO.getEventId());
            evalCriteria.setRound(round);
            evalCriteria.setCriteriaName(dto.getCriteriaName());
            evalCriteria.setDescription(dto.getDescription());
            evalCriteria.setWeight(dto.getWeight());
            // Lưu lại ID gốc nếu có thay đổi hoặc giữ nguyên
            if (dto.getCriteriaDetailId() != null) {
                evalCriteria.setCriteriaDetailId(dto.getCriteriaDetailId());
            }

            listToUpdate.add(evalCriteria);
        }
        evaluationCriteriaRepository.saveAll(listToUpdate);

    }


    //Xoa 1 tieu chi con trong bo tieu chi
    @Override
    public void deleteEvaluationCriteriaById(Integer evaluationCriteriaId) {
        EvaluationCriteria entity = evaluationCriteriaRepository.findById(evaluationCriteriaId)
                .orElseThrow(() -> new RuntimeException("EvaluationCriteria not found"));
        evaluationCriteriaRepository.delete(entity);

    }
    // xoa all tieu chi cua 1 round
    @Override
    public void deleteAllCriteriaByRoundId(Integer roundId) {
              // lay ds con ra xoa all
        List<EvaluationCriteria> listToDelete = evaluationCriteriaRepository.findByRound_RoundId(roundId);
        if (listToDelete != null && !listToDelete.isEmpty()) {
            evaluationCriteriaRepository.deleteAll(listToDelete);
        }

    }

}