package com.hackathon.dto.event;

import java.time.LocalDateTime;

public record UpdateTimeEventDTO(
        LocalDateTime registrationDeadline,
        LocalDateTime workshopTime,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
