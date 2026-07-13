package com.hackathon.service;

import com.hackathon.dto.expert.ExpertInfoResponse;
import com.hackathon.dto.expert.ExpertOverviewResponse;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.entity.Expert;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface ExpertService {
    List<ExpertInfoResponse> getAllExperts();
    ExpertInfoResponse getExpertById(Integer id);
    ExpertInfoResponse mapToResponse(Expert expert);
    ExpertOverviewResponse getExpertOverview(CustomUserDetails userDetails, Integer eventId);

}
