package com.hackathon.service;

import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.entity.Expert;

import java.util.List;

public interface ExpertService {
    List<ExpertInfoResponse> getAllExperts();
    ExpertInfoResponse getExpertById(Integer id);
    ExpertInfoResponse mapToResponse(Expert expert);
}
