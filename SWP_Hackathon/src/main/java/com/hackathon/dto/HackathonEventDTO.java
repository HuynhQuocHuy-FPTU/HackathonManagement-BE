package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class HackathonEventDTO {
    private Integer eventId;
    private String address;
    private String description;
    private LocalDateTime endDate;
    private String eventName;
    private String season;
    private LocalDateTime startDate;
    private String status;
    private String title;
    private Integer coordinatorId;


}
