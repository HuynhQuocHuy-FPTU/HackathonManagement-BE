package com.hackathon.service;

import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.category.CategoryRoundDTO;
import com.hackathon.dto.event.EventDTO;
import com.hackathon.dto.round.RoundDTO;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface ExpertAssignService {
    public void assignExpertsToCategoryRound(List<CategoryRound> saveCateRound, List<CategoryExpertAssignRequestDTO> requests, Round round);

    List<CategoryExpertAssignResponseDTO> getExpertAssignmentsByRound(Round round);

    void deleteByEventId(Integer eventId);

    List<EventDTO> getEventForJudge(CustomUserDetails userDetails);

    List<RoundDTO> getRoundForJudge(CustomUserDetails userDetails, Integer eventId);

    List<CategoryRoundDTO> getCategoryRoundForJudge(CustomUserDetails userDetails, Integer roundId);


}
