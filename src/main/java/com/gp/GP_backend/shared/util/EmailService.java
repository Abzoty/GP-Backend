package com.gp.GP_backend.shared.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails via the configured SMTP server.
 *
 * <h3>Async behavior</h3>
 * Every public method is annotated with {@code @Async}, meaning Spring executes
 * them
 * on a thread-pool thread rather than the calling request's thread. This
 * prevents a
 * slow or failing SMTP server from delaying or erroring the HTTP response.
 *
 * <p>
 * Requires {@code @EnableAsync} on
 * {@link com.gp.GP_backend.GpBackendApplication}.
 *
 * <h3>Configuration</h3>
 * SMTP credentials are set in {@code application-dev.properties} under
 * {@code spring.mail.*}. For Gmail, use an App Password (not the account
 * password)
 * and set {@code host=smtp.gmail.com}, NOT the email address itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a welcome email to a newly registered user.
     *
     * <p>
     * Failures are caught and logged — a broken SMTP configuration must not
     * prevent the user from completing registration.
     *
     * @param toEmail  recipient email address
     * @param fullName recipient's display name (used for personalisation)
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
            // Non-fatal — registration has already succeeded; log and continue
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}