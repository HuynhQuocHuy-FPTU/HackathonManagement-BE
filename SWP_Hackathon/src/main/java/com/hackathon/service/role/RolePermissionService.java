package com.hackathon.service.role;

import com.hackathon.dto.role.RolePermissionResponse;
import com.hackathon.dto.role.UpdateRolePermissionRequest;
import java.util.List;

public interface RolePermissionService {
    List<RolePermissionResponse> getAllRolePermissions();
    void updateRolePermissions(UpdateRolePermissionRequest request);
}