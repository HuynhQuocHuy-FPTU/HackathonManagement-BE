package com.hackathon.service;

import com.hackathon.email.MailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.dev-log-link:true}")
    private boolean devLogLink;

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = frontendUrl + "/verify-account?token=" + token;
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


    @Override
    public void sendEmail(MailRequest request, String templateName) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");

            Context context = new Context();
            context.setVariables(request.getProps());

            String html = templateEngine.process(templateName, context);

            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("Gửi Email thành công tới: " + request.getTo());

        } catch (Exception e) {
            System.out.println("Gửi Email thất bại tới: " + request.getTo() + e.getMessage());

        }
    }

    @Override
    public void sendLeaderTransferMail(MailRequest request, String templateName) {
        try {
            sendEmail(request, templateName);
        } catch (Exception ex) {
            System.out.println("Lỗi gửi Email(Transfer Leader)" + ex.getMessage());
        }

    }
}
