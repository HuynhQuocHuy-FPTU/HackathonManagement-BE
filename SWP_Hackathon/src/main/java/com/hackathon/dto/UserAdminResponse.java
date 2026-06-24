package com.hackathon.dto;

import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserAdminResponse {
    private int accountId;
    private String email;
    private String phone;
    private String fullName;
    private AccountRole role;
    private AccountStatus status;
    private String avatarUrl;

    // Các trường đặc thù theo Role
    private String university;
    private String organization;

    private LocalDateTime createdAt;
}