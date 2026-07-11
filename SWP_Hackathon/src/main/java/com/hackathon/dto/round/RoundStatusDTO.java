package com.hackathon.dto.round;

import com.hackathon.entity.enums.ParticipantStatus;
import lombok.Builder;
@Builder
public record RoundStatusDTO(Integer roundId, String roundName, ParticipantStatus status) {
}
