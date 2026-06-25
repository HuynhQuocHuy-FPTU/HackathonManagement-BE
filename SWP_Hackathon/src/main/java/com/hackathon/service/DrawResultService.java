package com.hackathon.service;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.TeamParticipant;

import java.util.List;

public interface DrawResultService {
    List<TeamParticipant> importDrawResults(Integer eventId, List<DrawResultRequestDTO> drawResults);
}
