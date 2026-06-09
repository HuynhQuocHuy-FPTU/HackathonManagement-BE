package com.hackathon.dto.event.response;

import com.hackathon.entity.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventDetailResponse {
    private int eventId;
    private String eventName;
    private String title;
    private String season;
    private String address;
    private String description;
    private Integer maxTeam;
    private Integer maxTeamSize;
    private Integer minTeamSize;
    private EventStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createAt;
    private LocalDateTime registrationDeadline;

}
