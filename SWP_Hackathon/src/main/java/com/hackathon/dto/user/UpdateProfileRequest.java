package com.hackathon.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String userName;

    @Pattern(regexp = "^(0|84)(2(0[3-9]|1[0-6|8|9]|2[0-2|5-9]|3[2-9]|4[0-9]|5[1|2|4-9]|6[0-3|9]|7[0-7]|8[0-9]|9[0-4|6|7|9])|3[2-9]|5[5|6|8|9]|7[0|6-9]|8[0-6|8|9]|9[0-4|6-9])([0-9]{7})$",
            message = "Số điện thoại không hợp lệ")
    private String phone;

    // --- CÁC TRƯỜNG DÀNH CHO STUDENT ---
    @Pattern(regexp = "^$|^[a-zA-Z0-9]+$", message = "Mã sinh viên chỉ được chứa chữ cái và số")
    @Size(max = 20, message = "Mã sinh viên không được vượt quá 20 ký tự")
    private String studentCode;

    @Size(max = 255, message = "Tên trường đại học không được vượt quá 255 ký tự")
    private String universityName;

    @Size(max = 255, message = "Chuyên ngành không được vượt quá 255 ký tự")
    private String major;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    // --- CÁC TRƯỜNG DÀNH CHO EXPERT / JUDGE ---
    @Size(max = 255, message = "Nơi làm việc không được vượt quá 255 ký tự")
    private String workplace;

    // --- TRƯỜNG DÙNG CHUNG CHO EXPERT VÀ EVENT_COORDINATOR ---
    @Size(max = 255, message = "Tên phòng ban / chuyên khoa không được vượt quá 255 ký tự")
    private String department;

    @Size(max = 255, message = "Tên tổ chức không được vượt quá 255 ký tự")
    private String organization;
}
