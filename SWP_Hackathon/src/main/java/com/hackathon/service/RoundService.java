package com.hackathon.service;

import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.entity.Round;
import com.hackathon.exception.BadRequestException;


public interface RoundService {
    public Round createRound(CreateRoundRequest request) throws BadRequestException;
}
