package com.hackathon.service;

import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;

import java.util.List;

public interface ExpertAssignService {
    public void assignExpertsToCategoryRound(List<CategoryRound> saveCateRound, List<CategoryExpertAssignRequestDTO> requests);

    List<CategoryExpertAssignResponseDTO> getExpertAssignmentsByRound(Round round);
}
