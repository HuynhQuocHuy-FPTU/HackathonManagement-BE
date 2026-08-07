package com.hackathon.service;

import com.hackathon.dto.team.AdvancedTeamDTO;
import com.hackathon.dto.team.CategoryAdvancementResultDTO;
import com.hackathon.entity.Round;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.security.CustomUserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface RoundAdvancementService {

    List<AdvancedTeamDTO> calculateScoresAndRanking(Integer categoryRoundId);

    void calculateRoundScoresAutomatically(Integer roundId);

    BigDecimal calculateTotalScore(TeamParticipant participant);

    List<TeamParticipant> calculateRanking(List<TeamParticipant> participants);

    List<AdvancedTeamDTO> advanceTopTeams(Integer currentCategoryRoundId);

    List<CategoryAdvancementResultDTO> advanceAllCategoriesInRound(
            Integer roundId,
            CustomUserDetails userDetails
    );

    List<CategoryAdvancementResultDTO> advanceRoundAutomatically(Integer roundId);

    void disqualifyRetroactively(
            TeamParticipant oldTeamParticipant,
            Round nextRound
    );

    LocalDateTime getFinalSubmissionTime(TeamParticipant participant);
}
