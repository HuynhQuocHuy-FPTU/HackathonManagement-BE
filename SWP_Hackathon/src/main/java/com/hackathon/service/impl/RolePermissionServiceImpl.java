package com.hackathon.service.impl;

import com.hackathon.dto.role.PermissionDto;
import com.hackathon.dto.role.RolePermissionResponse;
import com.hackathon.dto.role.UpdateRolePermissionRequest;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.ExpertAssignRepository;
import com.hackathon.repository.TeamMemberRepository;
import com.hackathon.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
// Quản lý ma trận quyền của từng vai trò và kiểm tra ảnh hưởng trước khi thay đổi quyền.
public class RolePermissionServiceImpl implements RolePermissionService {

    private final AccountRepository accountRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final TeamMemberRepository teamMemberRepository; // Inject thêm repo của TeamMember

    private static final Map<String, PermissionDto> permissionMatrix = new ConcurrentHashMap<>();
    private static final Map<String, String> displayNames = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // KHỞI TẠO 7 ROLE CHÍNH XÁC THEO GIAO DIỆN FRONTEND (Bỏ qua Expert chung)
    // -------------------------------------------------------------------------
    static {
        // Nhóm 1: Học sinh (Tách ảo thành 2 Role)
        initRole("TEAM_MEMBER", "Team member", new PermissionDto(false, false, false, false, false));
        initRole("TEAM_LEADER", "Team leader", new PermissionDto(true, false, false, false, false));

        // Nhóm 2: Chuyên gia (Sử dụng 3 role của ExpertAssign)
        initRole("GUEST_JUDGE", "Guest Judge", new PermissionDto(false, true, false, false, false));
        initRole("CORE_JUDGE", "Internal Judge", new PermissionDto(false, true, false, false, false));
        initRole("MENTOR", "Mentor", new PermissionDto(false, false, false, false, false));

        // Nhóm 3: Quản trị (Sử dụng Account Role)
        initRole("EVENTCOORDINATOR", "Coordinator", new PermissionDto(false, false, true, true, false));
        initRole("ADMIN", "Admin", new PermissionDto(true, false, true, true, true));
    }

    // Khởi tạo tên hiển thị và tập quyền mặc định cho một vai trò trong ma trận quyền.
    private static void initRole(String dbKey, String feName, PermissionDto permissions) {
        permissionMatrix.put(dbKey, permissions);
        displayNames.put(dbKey, feName);
    }

    // Trả về danh sách quyền hiện tại của tất cả vai trò theo tên dùng trên giao diện.
    @Override
    public List<RolePermissionResponse> getAllRolePermissions() {
        List<RolePermissionResponse> responses = new ArrayList<>();

        for (String roleKey : permissionMatrix.keySet()) {
            long count = 0;

            try {
                // ĐIỀU HƯỚNG LOGIC ĐẾM SỐ LƯỢNG (MEMBER COUNT)
                if (roleKey.equals("TEAM_LEADER")) {
                    count = teamMemberRepository.countByIsLeader(true);
                }
                else if (roleKey.equals("TEAM_MEMBER")) {
                    count = teamMemberRepository.countByIsLeader(false);
                }
                else if (roleKey.equals("ADMIN") || roleKey.equals("EVENTCOORDINATOR")) {
                    count = accountRepository.countByRole(AccountRole.valueOf(roleKey));
                }
                else if (isExpertRole(roleKey)) {
                    // Đếm MENTOR, GUEST_JUDGE, CORE_JUDGE (Dùng DISTINCT để không đếm trùng người)
                    count = expertAssignRepository.countDistinctExpertByRole(ExpertRole.valueOf(roleKey));
                }
            } catch (Exception e) {
                log.error("Lỗi khi đếm số lượng thành viên cho role: {}", roleKey, e);
            }

            responses.add(RolePermissionResponse.builder()
                    .role(displayNames.get(roleKey)) // VD: Trả về "Internal Judge"
                    .memberCount(count)
                    .permissions(permissionMatrix.get(roleKey)) // Cấu hình True/False
                    .build());
        }
        return responses;
    }

    // Kiểm tra yêu cầu thay đổi rồi cập nhật tập quyền tương ứng cho vai trò được chọn.
    @Override
    public void updateRolePermissions(UpdateRolePermissionRequest request) {
        String feRoleName = request.getRole().trim();
        String targetDbKey = null;

        // Quét từ điển để map tên Frontend (VD: "Internal Judge") về Key Database (VD: "CORE_JUDGE")
        for (Map.Entry<String, String> entry : displayNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(feRoleName)) {
                targetDbKey = entry.getKey();
                break;
            }
        }

        if (targetDbKey == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy cấu hình cho vai trò: " + feRoleName);
        }

        // Cập nhật ma trận quyền trong RAM
        permissionMatrix.put(targetDbKey, request.getPermissions());
    }

    // --- Hàm tiện ích kiểm tra xem role có thuộc nhóm Expert không ---
    private boolean isExpertRole(String role) {
        for (ExpertRole r : ExpertRole.values()) {
            if (r.name().equals(role)) return true;
        }
        return false;
    }
}
