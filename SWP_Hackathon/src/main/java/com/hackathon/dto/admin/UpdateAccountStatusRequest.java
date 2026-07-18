package com.hackathon.dto.admin;

import com.hackathon.entity.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAccountStatusRequest {
    @NotNull(message = "Trạng thái tài khoản không được để trống")
    private AccountStatus status;
}