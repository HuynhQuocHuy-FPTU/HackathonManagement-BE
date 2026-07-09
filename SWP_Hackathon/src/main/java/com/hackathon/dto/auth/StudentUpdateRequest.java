package com.hackathon.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentUpdateRequest {
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại phải gồm đúng 10 chữ số")
    private String phone;

    @NotBlank(message = "Mã số sinh viên không được để trống")
    @Size(max = 20, message = "Mã số sinh viên tối đa 20 ký tự")
    private String studentCode;

    @NotBlank(message = "Tên sinh viên không được để trống")
    @Size(max = 50, message = "Tên sinh viên tối đa 50 ký tự")
    private String studentName;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @NotBlank(message = "Chuyên ngành không được để trống")
    @Size(max = 255, message = "Chuyên ngành tối đa 255 ký tự")
    private String major;

    @NotBlank(message = "University không được để trống")
    @Size(max = 255, message = "University tối đa 255 ký tự")
    private String university;
}
