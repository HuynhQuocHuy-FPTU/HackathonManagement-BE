//package com.hackathon.dto.auth;
//
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//import lombok.Getter;
//import lombok.Setter;
//
//@Setter
//@Getter
//public class ChangeFirstPasswordRequest {
//    @NotBlank(message = "Email không được để trống")
//    @Email(message = "Email không hợp lệ")
//    private String email;
//
//    @NotBlank(message = "Mật khẩu không được để trống")
//    @Pattern(
//            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
//            message = "Mật khẩu tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@$!%*?&)"
//    )
//    private String password;
//}
