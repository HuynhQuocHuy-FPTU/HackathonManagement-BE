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
public class RegistrationDTO {
    private Integer registrationId;
    private LocalDateTime registrationDate;
    private String status;
    private Integer categoryId;
    private Integer eventId;
    private Integer teamId;

    }
