package com.hackathon.dto;

import lombok.*;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class AccountDTO {
    private Integer accountId;
    private String accountName;
    private LocalDateTime createdAt;
    private String email;
    private String password;
    private String phone;
    private String role;
    private String status;
    private LocalDateTime updatedAt;


}
