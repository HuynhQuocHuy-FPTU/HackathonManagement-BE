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
public class EventResponse {
    private int eventId;
    private String eventName;
    private String title;
    private String season;
    private String address;
    private EventStatus status;
    private LocalDateTime registrationDeadline;



}

