package com.hackathon.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathon.entity.enums.AccountRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    // ==========================================
    // THÔNG TIN TÀI KHOẢN CHUNG (Tất cả Role)
    // ==========================================
    private int accountId;
    private AccountRole role;
    private String email;
    private String avatarUrl;
    private String displayName; // Tên hiển thị chung trên UI Frontend
    private String githubUsername;
    private String githubUrl;

    // ==========================================
    // THÔNG TIN DÀNH CHO SINH VIÊN
    // ==========================================
    private String studentCode;
    private String universityName;
    private String major;
    private String address;

    // ==========================================
    // THÔNG TIN DÀNH CHO CHUYÊN GIA / BAN TỔ CHỨC
    // ==========================================
    private String department;
    private String organization;
}