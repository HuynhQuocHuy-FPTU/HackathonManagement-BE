package com.hackathon.dto.participant;

import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.SubmissionStatus;

import java.math.BigDecimal;

public record ParticipantDetailDTO(
        Integer participantId,
        Integer registrationId,
        Integer teamID,
        String teamName,
        ParticipantStatus status,
        String disqualifyReason,
        SubmissionStatus submissionStatus,
        BigDecimal totalScore,
        Integer rank) {
}
