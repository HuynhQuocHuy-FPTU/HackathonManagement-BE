package com.hackathon.controller;

import com.hackathon.dto.role.RolePermissionResponse;
import com.hackathon.dto.role.UpdateRolePermissionRequest;
import com.hackathon.exception.ApiResponse;
import com.hackathon.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RolePermissionService rolePermissionService;

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RolePermissionResponse>>> getRolePermissions() {
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách phân quyền thành công", rolePermissionService.getAllRolePermissions()));
    }

    @PutMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @Valid @RequestBody UpdateRolePermissionRequest request) {
        rolePermissionService.updateRolePermissions(request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật quyền của " + request.getRole() + " thành công!"));
    }
}