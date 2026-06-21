package com.hackathon.dto.student;

import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentHistoryResponse {
    private String eventName;
    private String teamName;
    private boolean role;
    private LocalDateTime joinedAt;
    private String ranking;
    private String reward;
    private String description;

}
