package com.hackathon.dto.role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolePermissionResponse {
    private String role;
    private long memberCount;
    private PermissionDto permissions;
}