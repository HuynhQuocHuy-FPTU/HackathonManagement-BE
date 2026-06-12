package com.hackathon.service;

import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import com.hackathon.exception.BadRequestException;

import java.util.List;


public interface RoundService {
    public Round createRound(CreateRoundRequest request, int eventId) throws BadRequestException;
    public RoundResponse mapToResponse(Round round);

}
