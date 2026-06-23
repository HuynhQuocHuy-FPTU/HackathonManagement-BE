package com.hackathon.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String userName;

    @Pattern(regexp = "^(0|84)(2(0[3-9]|1[0-6|8|9]|2[0-2|5-9]|3[2-9]|4[0-9]|5[1|2|4-9]|6[0-3|9]|7[0-7]|8[0-9]|9[0-4|6|7|9])|3[2-9]|5[5|6|8|9]|7[0|6-9]|8[0-6|8|9]|9[0-4|6-9])([0-9]{7})$",
            message = "Số điện thoại không hợp lệ")
    private String phone;

    // --- CÁC TRƯỜNG DÀNH CHO STUDENT ---
    private String studentCode;
    private String address;
    private String major;

    // --- CÁC TRƯỜNG DÀNH CHO EXPERT / JUDGE ---
    private String workplace;

    // --- TRƯỜNG DÙNG CHUNG CHO EXPERT VÀ EVENT_COORDINATOR ---
    private String department;
}
