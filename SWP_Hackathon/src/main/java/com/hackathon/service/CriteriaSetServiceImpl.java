package com.hackathon.service;

import com.hackathon.dto.criteria.CriteriaSetDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import com.hackathon.entity.CriteriaSet;
import com.hackathon.repository.CriteriaSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;




@Service
public class CriteriaSetServiceImpl implements CriteriaSetService {
    @Autowired
    private CriteriaSetRepository criteriaSetRepository;


    @Override
    public CriteriaSetResponseDTO getCriteriaSetById(Integer criteriaSetId) {
        CriteriaSet criteriaSet = criteriaSetRepository.findByCriteriaSetId(criteriaSetId);
        if(criteriaSet!=null){
            CriteriaSetResponseDTO dto = new  CriteriaSetResponseDTO(
                    criteriaSet.getCriteriaSetId(),
                    criteriaSet.getCriteriaSetName(),
                    criteriaSet.getMaxScore()
            );
            return dto;
        }
        return null;
    }

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

    @Override
    public List<CriteriaSetDetailResponseDTO> getCriteriaDetailById(Integer criteriaSetId) {
        CriteriaSet criteriaSet=criteriaSetRepository.findByCriteriaSetId(criteriaSetId);
        List<CriteriaSetDetailResponseDTO> dto = criteriaSet.getCriteriaDetails().stream()
                .map(criteriaDetail -> new CriteriaSetDetailResponseDTO(
                        criteriaDetail.getCriteriaId(),
                        criteriaDetail.getCriteriaName(),
                        criteriaDetail.getWeight()
                ))
                .toList();
        return dto;
    }



}
