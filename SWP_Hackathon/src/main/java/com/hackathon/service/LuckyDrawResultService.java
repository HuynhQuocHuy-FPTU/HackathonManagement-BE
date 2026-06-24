package com.hackathon.service;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface LuckyDrawResultService {
    List<TeamParticipant> importDrawResults(Integer eventId, DrawResultRequestDTO drawResults, CustomUserDetails userDetails, Integer responseDeadline);
}
