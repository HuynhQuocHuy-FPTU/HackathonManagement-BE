package com.hackathon.service;

import com.hackathon.dto.criteria.CriteriaDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import com.hackathon.entity.CriteriaDetail;
import com.hackathon.entity.CriteriaSet;
import com.hackathon.repository.CriteriaDetailRepository;
import com.hackathon.repository.CriteriaSetRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CriteriaSetServiceImpl implements CriteriaSetService {
    @Autowired
    private CriteriaSetRepository criteriaSetRepository;
    @Autowired
    private CriteriaDetailRepository criteriaDetailRepository;


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

    // 2.  Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    @Override
    @Transactional
    public List<CriteriaSetDetailResponseDTO> getAllCriteriaSetDetail() {

        List<CriteriaSet> sets = criteriaSetRepository.findAll();

        return sets.stream().map(set -> {

            CriteriaSetDetailResponseDTO dto = new CriteriaSetDetailResponseDTO();

            dto.setCriteriaSetId(set.getCriteriaSetId());
            dto.setCriteriaSetName(set.getCriteriaSetName());
            dto.setWeight(set.getMaxScore());

            List<CriteriaDetailResponseDTO> details = set.getCriteriaDetails()
                    .stream()
                    .map(d -> new CriteriaDetailResponseDTO(
                            d.getCriteriaId(),
                            d.getCriteriaName(),
                            d.getWeight()
                    ))
                    .toList();

            dto.setCriteriaDetails(details);

            return dto;
        }).toList();
    }

    //3. Lay tat ca thong tin trong tieu chi chi tiet(detail) hien thi
    @Override
    public List<CriteriaDetailResponseDTO> getAllCriteriaDetail() {

        return criteriaDetailRepository.findAll()
                .stream()
                .map(cri -> new CriteriaDetailResponseDTO(
                        cri.getCriteriaId(),
                        cri.getCriteriaName(),
                        cri.getWeight()
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
                cri.getWeight()
        )).toList();

    }

}
