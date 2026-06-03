package com.hackathon.dto.event;

import java.time.LocalDateTime;

public class CreateEventRequest {
    private String eventName;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String title;

    private String address;

    private String description;

    private String season;
}
