package com.hackathon.dto.role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRolePermissionRequest {
    @NotBlank(message = "Tên vai trò không được để trống")
    private String role;

    @NotNull(message = "Danh sách quyền không được để trống")
    @Valid
    private PermissionDto permissions;
}