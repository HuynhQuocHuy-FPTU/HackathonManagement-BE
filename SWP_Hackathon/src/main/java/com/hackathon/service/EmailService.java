package com.hackathon.service;

import com.hackathon.email.MailRequest;
import jakarta.mail.MessagingException;

public interface EmailService {
    void sendEmail(MailRequest request, String templateName) throws MessagingException;
    void sendTemporaryPasswordEmail(String toEmail, String tempPassword, String fullName);
    void sendVerificationEmail(String toEmail, String token);
    void sendForgotPasswordEmail(String toEmail, String otp);
    void sendGeneralEmail(String toEmail, String tile, String emailMessage);
    void sendNotifyToExpertReEvaluation(String email, String teamName);
}
