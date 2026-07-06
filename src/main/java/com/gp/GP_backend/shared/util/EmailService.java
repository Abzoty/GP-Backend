package com.gp.GP_backend.shared.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to Covalent 🎉");
            message.setText(
                    "Hi " + fullName + ",\n\n" +
                            "Welcome to Covalent! Your account has been created successfully.\n\n" +
                            "Start exploring spaces, asking questions, and earning XP!\n\n" +
                            "— The Covalent Team");
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            // Email failure must not break registration
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Reset your Covalent password");
            message.setText(
                    "Hi " + fullName + ",\n\n" +
                            "We received a request to reset your password. Click the link below to proceed:\n\n" +
                            resetLink + "\n\n" +
                            "This link expires in 15 minutes and can only be used once.\n\n" +
                            "If you did not request a password reset, you can safely ignore this email.\n\n" +
                            "— The Covalent Team");
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            // Email failure is logged; the service layer already committed the token to the
            // DB
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}