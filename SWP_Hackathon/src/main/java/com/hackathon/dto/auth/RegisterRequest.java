package com.hackathon.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Mật khẩu tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@$!%*?&)"
    )
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (Phải bắt đầu bằng 0 hoặc +84 và theo sau là 9 chữ số)")
    private String phone;

    @NotBlank(message = "Mã số sinh viên không được để trống")
    @Size(max = 20, message = "Mã số sinh viên tối đa 20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Mã số sinh viên chỉ được chứa chữ cái và số, không chứa khoảng trắng hoặc ký tự đặc biệt")
    private String studentCode;

    @NotBlank(message = "Tên sinh viên không được để trống")
    @Size(max = 50, message = "Tên sinh viên tối đa 50 ký tự")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Tên sinh viên chỉ được chứa chữ cái và khoảng trắng, không chứa số hay ký tự đặc biệt")
    private String studentName;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @NotBlank(message = "Chuyên ngành không được để trống")
    @Size(max = 255, message = "Chuyên ngành tối đa 255 ký tự")
    private String major;

    @NotBlank(message = "University không được để trống")
    @Size(max = 255, message = "Tên trường đại học tối đa 255 ký tự")
    private String university;
}