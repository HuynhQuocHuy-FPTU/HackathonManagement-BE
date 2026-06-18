//package com.hackathon.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
//
//import java.util.Properties;
//
//@Configuration
//public class MailConfig {
//    @Value("${mailServer.host}")
//    private String host;
//
//    @Value("${mailServer.port}")
//    private Integer port;
//
//    @Value("${mailServer.email}")
//    private String email;
//
//    @Bean
//    public JavaMailSender javaMailSender() {
//
//
//
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//
//        mailSender.setHost("smtp.gmail.com");
//        mailSender.setPort(587);
//
//        mailSender.setUsername("your_email@gmail.com");
//        mailSender.setPassword("your_app_password");
//
//        Properties props = mailSender.getJavaMailProperties();
//
//        props.put("mail.transport.protocol", "smtp");
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.debug", "true");
//
//        return mailSender;
//    }
//}
