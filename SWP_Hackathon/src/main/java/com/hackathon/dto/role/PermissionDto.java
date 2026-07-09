package com.hackathon.dto.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private boolean canSubmit;
    private boolean canEvaluate;
    private boolean canManageUsers;
    private boolean canCreateEvents;
    private boolean canExportSystemLogs;
}