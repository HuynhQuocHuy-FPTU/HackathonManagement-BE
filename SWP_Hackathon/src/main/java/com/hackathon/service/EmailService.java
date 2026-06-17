package com.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.dev-log-link:true}")
    private boolean devLogLink;

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = frontendUrl + "/account/verify-email?token=" + token;
        String subject = "Xác thực tài khoản Hackathon";
        String body = """
                Xin chào,

                Vui lòng nhấn vào liên kết sau để xác thực email và kích hoạt tài khoản:
                %s

                Liên kết có hiệu lực trong 24 giờ.

                Trân trọng,
                Ban tổ chức Hackathon
                """.formatted(verifyUrl);

        if (!StringUtils.hasText(mailUsername)) {
            if (devLogLink) {
                log.info("=== DEV: Verification link for {} ===\n{}", toEmail, verifyUrl);
            }
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendTemporaryPasswordEmail(String toEmail, String tempPassword, String fullName){
        String loginUrl = frontendUrl + "/login";
        String subject = "[Hackathon System] Thông tin cấp tài khoản thành viên mới";
        String body = """
                Xin chào %s,
                
                Bạn đã được Ban tổ chức cấp tài khoản thành viên trên hệ thống quản lý Hackathon.
                Dưới đây là thông tin đăng nhập của bạn:
                - Đường dẫn hệ thống: %s
                - Tài khoản (Email): %s
                - Mật khẩu tạm thời: %s
                
                LƯU Ý BẢO MẬT: 
                Để đảm bảo an toàn, bạn bắt buộc phải thực hiện đổi mật khẩu trong lần đầu tiên đăng nhập thành công vào hệ thống.
                
                Trân trọng,
                Ban tổ chức giải đấu.
                """.formatted(fullName, loginUrl, toEmail, tempPassword);

        if (!StringUtils.hasText(mailUsername)) {
            if (devLogLink) {
                log.info("=== DEV: Temporary password for {} ===\nPassword: {}\nLogin URL: {}", toEmail, tempPassword, loginUrl);
            }
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
