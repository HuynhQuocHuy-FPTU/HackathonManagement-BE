package com.hackathon.dto.round;

import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.entity.enums.SubmissionType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RoundStatusDTO(
        Integer roundId,
        Integer categoryRound,
        String categoryName,
        String roundName,
        SubmissionType submissionType,
        ParticipantStatus status,
        RoundStatus roundStatus,
        LocalDateTime SubmissionDeadline,
        LocalDateTime StartTime,
        LocalDateTime EndTime,
        List<EvaluationCriteria> evaluetionCriteria
) {
}
