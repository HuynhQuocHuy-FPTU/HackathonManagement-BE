package com.hackathon.service;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.Participant;

import java.util.List;

public interface DrawResultService {
    List<Participant> importDrawResults(Integer eventId, List<DrawResultRequestDTO> drawResults);
}
