package com.hackathon.dto.round;

import java.time.LocalDateTime;

public class CreateRoundRequest {
    private String roundName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String advancementRule;
}
