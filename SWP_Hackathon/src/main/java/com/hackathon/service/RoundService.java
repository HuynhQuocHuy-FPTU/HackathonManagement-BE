package com.hackathon.service;

import com.hackathon.dto.category.UpdateCategoryRequest;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.dto.round.UpdateRoundRequest;
import com.hackathon.dto.round.UpdateTimeRoundRequest;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.security.CustomUserDetails;

import java.util.List;
import java.util.Optional;


public interface RoundService {
    public Round createRound(CreateRoundRequest request, int eventId) throws BadRequestException;
    public RoundResponse mapToResponse(Round round);

    public Round updateSingleRound(UpdateRoundRequest roundRequest, List<Round> currentRounds, Integer eventId);

    public List<Round> deleteRoundsExcluding(List<UpdateRoundRequest> roundRequests, List<Round> currentRounds);

    void deleteByEventId(Integer eventId);

    List<Round> findAllByEventId(Integer eventId);

    List<Round> getRoundByStatusNot(RoundStatus status);

    Round saveRound(Round round);

    Optional<Round> findById(Integer roundId);

    void updateTimeRound(UpdateTimeRoundRequest updateTimeRoundRequest, CustomUserDetails userDetails, Integer roundId);

}
