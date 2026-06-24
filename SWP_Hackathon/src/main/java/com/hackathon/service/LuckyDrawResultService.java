package com.hackathon.service;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.TeamParticipation;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface LuckyDrawResultService {
    List<TeamParticipation> importDrawResults(Integer eventId, DrawResultRequestDTO drawResults, CustomUserDetails userDetails, Integer responseDeadline);
}
