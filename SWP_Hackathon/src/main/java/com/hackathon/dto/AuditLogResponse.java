package com.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathon.entity.enums.AccountRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AuditLogResponse {

    private Long id;
    private Integer accountId;
    private String actorName;
    private AccountRole role;
    private String action;
    private String entityType;
    private Integer entityId;
    private String message;
    private Object data;
    private LocalDateTime createdAt;
}
