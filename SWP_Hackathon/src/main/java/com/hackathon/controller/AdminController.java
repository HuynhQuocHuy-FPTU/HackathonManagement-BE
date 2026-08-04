package com.hackathon.controller;

import com.hackathon.dto.AdminOverviewResponse;
import com.hackathon.dto.AuditLogResponse;
import com.hackathon.dto.UserAdminResponse;
import com.hackathon.dto.admin.InviteAccountRequest;
import com.hackathon.dto.admin.UpdateAccountStatusRequest;
import com.hackathon.entity.enums.SystemConfigKey;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.impl.AuditService;
import com.hackathon.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.hackathon.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    private final AuditService auditService;
    private final SystemConfigService systemConfigService;

    @GetMapping("/auditLog")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLog(
            @PageableDefault(page = 0, size = 50, sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable) {
        Page<AuditLogResponse> list = auditService.getAllAuditLog(pageable);
        return ResponseEntity.ok(list);
    }
    /**
     * API: Lấy danh sách toàn bộ người dùng
     * GET /api/admin/users
     */
    @GetMapping("/users")
    // Tương lai bạn có thể thêm @PreAuthorize("hasAuthority('ADMIN')") ở đây để bảo mật
    public ResponseEntity<ApiResponse<List<UserAdminResponse>>> getAllUsers() {
        List<UserAdminResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách người dùng thành công", users));
    }

    /**
     * API: Lấy thông tin chi tiết một người dùng
     * GET /api/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserAdminResponse>> getUserById(@PathVariable int id) {
        UserAdminResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin chi tiết người dùng thành công", user));
    }

    /**
     * API: Admin tạo tài khoản cho Expert hoặc EventCoordinator
     * POST /api/admin/invite
     */
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> inviteAccount(@Valid @RequestBody InviteAccountRequest request) {
        adminService.inviteAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã tạo tài khoản và gửi email chứa mật khẩu tạm thời thành công!"));
    }

    /**
     * API: Cập nhật trạng thái hoạt động của tài khoản (Khóa / Mở khóa)
     * PATCH /api/admin/users/{id}/status
     */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable int id,
            @Valid @RequestBody UpdateAccountStatusRequest request) {

        adminService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật trạng thái tài khoản thành công!"));
    }

    /**
     * API: ADMIN XEM TỔNG QUAN
     *
     */

    @GetMapping("/overviews")
    public ResponseEntity<ApiResponse<AdminOverviewResponse>> getOverviewForAdmin() {
        AdminOverviewResponse overViews = adminService.getOverviewForAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Admin xem thông tin thành công", overViews));
    }

    @PutMapping("/system-config")
    public ResponseEntity<String> updateSystemConfig(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam SystemConfigKey key,
            @RequestParam Integer value) {
        systemConfigService.updateSystemConfig(userDetails, value, key);
        return ResponseEntity.ok("Cập nhật cấu hình thành công.");
    }


//    private final AdminService adminService;
//    @PostMapping("invite")
//    public ResponseEntity<String> inviteAccount(@Valid @RequestBody InviteAccountRequest request){
//        String result = adminService.inviteAccountByAdmin(request);
//        return ResponseEntity.ok(result);
//    }

}
