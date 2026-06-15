package com.hackathon.service;

import com.hackathon.email.MailRequest;
import jakarta.mail.MessagingException;

public interface EmailService {
    void sendEmail(MailRequest request, String templateName) throws MessagingException;
    void sendLeaderTransferMail(MailRequest request, String templateName);
}
