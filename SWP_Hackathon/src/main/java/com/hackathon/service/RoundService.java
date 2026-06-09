package com.hackathon.service;

import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.Round;
import com.hackathon.exception.BadRequestException;

import java.util.List;


public interface RoundService {
    public Round createRound(CreateRoundRequest request) throws BadRequestException;
    public RoundResponse mapToResponse(Round round, List<String> appliedCategoryName);
}
