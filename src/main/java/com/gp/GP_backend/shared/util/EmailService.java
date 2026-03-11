package com.gp.GP_backend.shared.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails using Spring's {@link JavaMailSender}.
 *
 * <p>
 * All methods are annotated with {@code @Async} so they run on a separate
 * thread pool and do not block the HTTP request thread. A failed email send
 * is logged as an error but never propagates to the caller.
 *
 * <p>
 * SMTP credentials are configured in {@code application-dev.properties}.
 * In production, store credentials in environment variables or a secrets
 * manager.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a welcome email to a newly registered user.
     * Runs asynchronously — the user's registration response is not delayed by this
     * call.
     *
     * @param toEmail  the recipient's email address.
     * @param fullName the recipient's display name, used for personalisation.
     */
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
            // Log and swallow — email failure must not break registration
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}
