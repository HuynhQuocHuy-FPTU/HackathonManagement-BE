package com.hackathon.service;

import com.hackathon.dto.criteria.*;
import com.hackathon.entity.CriteriaDetail;
import com.hackathon.entity.CriteriaSet;
import com.hackathon.repository.CriteriaDetailRepository;
import com.hackathon.repository.CriteriaSetRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CriteriaSetServiceImpl implements CriteriaSetService {

    private final CriteriaSetRepository criteriaSetRepository;
    private final CriteriaDetailRepository criteriaDetailRepository;

    //1.Lay all thong tin trong Set
    @Override
    public List<CriteriaSetResponseDTO> getAllCriteriaSets() {
        List<CriteriaSet> criteriaSets = criteriaSetRepository.findAll();
        return criteriaSets.stream()
                .map(criteriaSet -> new CriteriaSetResponseDTO(
                        criteriaSet.getCriteriaSetId(),
                        criteriaSet.getCriteriaSetName(),
                        criteriaSet.getMaxScore()
                ))
                .toList();
    }

    // 3.  Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    @Override
    @Transactional
    public List<CriteriaSetDetailResponseDTO> getAllCriteriaSetDetail() {

        List<CriteriaSet> sets = criteriaSetRepository.findAll();

        return sets.stream().map(set -> {

            CriteriaSetDetailResponseDTO dto = new CriteriaSetDetailResponseDTO();

            dto.setCriteriaSetId(set.getCriteriaSetId());
            dto.setCriteriaSetName(set.getCriteriaSetName());
            dto.setMaxScore(set.getMaxScore());

            List<CriteriaDetailResponseDTO> details = set.getCriteriaDetails()
                    .stream()
                    .map(d -> new CriteriaDetailResponseDTO(
                            d.getCriteriaId(),
                            d.getCriteriaName(),
                            d.getWeight(),
                            d.getDescription()
                    ))
                    .toList();

            dto.setCriteriaDetails(details);

            return dto;
        }).toList();
    }

    //2. Lay tat ca thong tin trong tieu chi chi tiet(detail) hien thi
    @Override
    public List<CriteriaDetailResponseDTO> getAllCriteriaDetail() {

        return criteriaDetailRepository.findAll()
                .stream()
                .map(cri -> new CriteriaDetailResponseDTO(
                        cri.getCriteriaId(),
                        cri.getCriteriaName(),
                        cri.getWeight(),
                        cri.getDescription()
                ))
                .toList();
    }
    //4.Thong qua ID Cua criteriaSet lay duoc ds criteriaDetail tuong ung vs id cua Set

    @Override
    public List<CriteriaDetailResponseDTO> getCriteriaDetailById(Integer criteriaSetId) {
        List<CriteriaDetail> details = criteriaDetailRepository.findByCriteriaSet_CriteriaSetId(criteriaSetId);
        if (details.isEmpty()) {
            throw new EntityNotFoundException(
                    "No criteria found for Criteria Set ID: " + criteriaSetId
            );
        }
        return details.stream().map(cri -> new CriteriaDetailResponseDTO(
                cri.getCriteriaId(),
                cri.getCriteriaName(),
                cri.getWeight(),
                cri.getDescription()
        )).toList();

    }

    @Override
    public CriteriaSetResponseDTO createCriteriaSet(CriteriaSetRequestDTO request) {
        // 1. Tao CriteriaSet
        CriteriaSet criteriaSet = new CriteriaSet();
        criteriaSet.setCriteriaSetName(request.getCriteriaSetName());
        criteriaSet.setMaxScore(request.getMaxScore());

        // 2.Tao 1 list de luu Criteria-detail
        List<CriteriaDetail> list = new ArrayList<>();
        for (CriteriaDetailRequestDTO dto : request.getCriteriaDetails()) {
            CriteriaDetail detail = new CriteriaDetail();
            detail.setCriteriaName(dto.getCriteriaName());
            detail.setWeight(dto.getWeight());
            detail.setDescription(dto.getDescription());
            detail.setCriteriaSet(criteriaSet);
            list.add(detail);
        }
        criteriaSet.setCriteriaDetails(list);
        // 3. Luu du lieu xuong DB
        CriteriaSet saved = criteriaSetRepository.save(criteriaSet);

        // 4. Tra du lieu ve DTO
        CriteriaSetResponseDTO response = new CriteriaSetResponseDTO();
        response.setCriteriaSetId(saved.getCriteriaSetId());
        response.setCriteriaName(saved.getCriteriaSetName());
        response.setMaxScore(saved.getMaxScore());
        return response;
    }

    @Override
    public CriteriaSetResponseDTO updateCriteriaSet(CriteriaSetRequestDTO request) {
        // Lay bo tieu chi can update
        CriteriaSet criteriaSet = criteriaSetRepository
                .findByCriteriaSetId(request.getCriteriaSetId());

        if (criteriaSet == null) {
            throw new RuntimeException("CriteriaSet not found with id: " + request.getCriteriaSetId());
        }
        criteriaSet.setCriteriaSetName(request.getCriteriaSetName());
        criteriaSet.setMaxScore(request.getMaxScore());

        CriteriaSet saved = criteriaSetRepository.save(criteriaSet);

        CriteriaSetResponseDTO response = new CriteriaSetResponseDTO();
        response.setCriteriaSetId(saved.getCriteriaSetId());
        response.setCriteriaName(saved.getCriteriaSetName());
        response.setMaxScore(saved.getMaxScore());
        return response;
    }

    // Xoa bo tieu chi
    @Override
    public void deleteCriteriaSet(Integer criteriaSetId) {
        CriteriaSet criteriaSet = criteriaSetRepository.findByCriteriaSetId(criteriaSetId);
        if (criteriaSet == null) {
            throw new RuntimeException("CriteriaSet not found with id: " + criteriaSetId);
        }
        criteriaSetRepository.delete(criteriaSet);

    }


}
