package com.hackathon.service;

import com.hackathon.dto.UserAdminResponse;
import com.hackathon.dto.admin.InviteAccountRequest;

import java.util.List;

public interface AdminService {
    /**
     * Lấy danh sách toàn bộ người dùng trong hệ thống
     */
    List<UserAdminResponse> getAllUsers();

    /**
     * Lấy thông tin chi tiết của một người dùng dựa vào ID
     * @param id ID của tài khoản cần tìm
     */
    UserAdminResponse getUserById(int id);
    void inviteAccount(InviteAccountRequest request);
}