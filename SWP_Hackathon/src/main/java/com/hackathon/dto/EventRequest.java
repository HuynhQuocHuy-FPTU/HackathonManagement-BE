package com.hackathon.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {

    private String eventName;
    private String description;
    private String address;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

}
