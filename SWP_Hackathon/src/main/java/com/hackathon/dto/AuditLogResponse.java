package com.hackathon.dto;

import com.hackathon.entity.enums.AccountRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AuditLogResponse {

    private Long id;
    private String actorName;
    private AccountRole role;
    private String action;
    private String entityType;
    private Integer entityId;
    private String message;
    private LocalDateTime createdAt;
}
