package com.hackathon.dto.registration;

import com.hackathon.entity.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class RegistrationHistoryResponse {
    private Integer registrationId;
    private Integer eventId;
    private String eventName;
    private Integer teamId;
    private String teamName;
    private Integer teamSize;
    private LocalDateTime registrationDate;
    private RegistrationStatus status;
}
