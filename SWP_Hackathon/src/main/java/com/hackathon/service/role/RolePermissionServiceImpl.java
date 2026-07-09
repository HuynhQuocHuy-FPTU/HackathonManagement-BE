package com.hackathon.service.role;

import com.hackathon.dto.role.PermissionDto;
import com.hackathon.dto.role.RolePermissionResponse;
import com.hackathon.dto.role.UpdateRolePermissionRequest;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.ExpertAssignRepository;
import com.hackathon.repository.TeamMemberRepository;
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
public class RolePermissionServiceImpl implements RolePermissionService {

    private final AccountRepository accountRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final TeamMemberRepository teamMemberRepository;

    private static final Map<String, PermissionDto> permissionMatrix = new ConcurrentHashMap<>();
    private static final Map<String, String> displayNames = new ConcurrentHashMap<>();

    // Khởi tạo dữ liệu gốc cho 7 Role khớp với Giao diện Frontend
    static {
        initRole("TEAM_MEMBER", "Team member", new PermissionDto(false, false, false, false, false));
        initRole("TEAM_LEADER", "Team leader", new PermissionDto(true, false, false, false, false));
        initRole("GUEST_JUDGE", "Guest Judge", new PermissionDto(false, true, false, false, false));
        initRole("CORE_JUDGE", "Internal Judge", new PermissionDto(false, true, false, false, false));
        initRole("MENTOR", "Mentor", new PermissionDto(false, false, false, false, false));
        initRole("EVENTCOORDINATOR", "Coordinator", new PermissionDto(false, false, true, true, false));
        initRole("ADMIN", "Admin", new PermissionDto(true, true, true, true, true));
    }

    private static void initRole(String dbKey, String feName, PermissionDto permissions) {
        permissionMatrix.put(dbKey, permissions);
        displayNames.put(dbKey, feName);
    }

    @Override
    public List<RolePermissionResponse> getAllRolePermissions() {
        List<RolePermissionResponse> responses = new ArrayList<>();

        for (String roleKey : permissionMatrix.keySet()) {
            long count = 0;
            try {
                if (roleKey.equals("TEAM_LEADER")) {
                    count = teamMemberRepository.countByIsLeader(true);
                } else if (roleKey.equals("TEAM_MEMBER")) {
                    count = teamMemberRepository.countByIsLeader(false);
                } else if (roleKey.equals("ADMIN") || roleKey.equals("EVENTCOORDINATOR")) {
                    count = accountRepository.countByRole(AccountRole.valueOf(roleKey));
                } else if (isExpertRole(roleKey)) {
                    count = expertAssignRepository.countDistinctExpertByRole(ExpertRole.valueOf(roleKey));
                }
            } catch (Exception e) {
                log.error("Lỗi khi đếm số lượng thành viên cho role: {}", roleKey, e);
            }

            responses.add(RolePermissionResponse.builder()
                    .role(displayNames.get(roleKey)) // Trả về tên hiển thị (VD: "Internal Judge")
                    .memberCount(count)
                    .permissions(permissionMatrix.get(roleKey))
                    .build());
        }
        return responses; // Danh sách 7 object gửi cho FE
    }

    @Override
    public void updateRolePermissions(UpdateRolePermissionRequest request) {
        String feRoleName = request.getRole().trim();
        String targetDbKey = null;

        // Dịch tên từ Frontend (Ví dụ: "Internal Judge") thành Database Key (Ví dụ: "CORE_JUDGE")
        for (Map.Entry<String, String> entry : displayNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(feRoleName)) {
                targetDbKey = entry.getKey();
                break;
            }
        }

        if (targetDbKey == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy cấu hình cho vai trò: " + feRoleName);
        }

        permissionMatrix.put(targetDbKey, request.getPermissions());
    }

    private boolean isExpertRole(String role) {
        for (ExpertRole r : ExpertRole.values()) {
            if (r.name().equals(role)) return true;
        }
        return false;
    }
}