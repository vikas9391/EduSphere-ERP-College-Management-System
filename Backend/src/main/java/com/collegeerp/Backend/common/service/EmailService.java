package com.collegeerp.Backend.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring's {@link JavaMailSender} (spring-boot-starter-mail was
 * already a dependency in pom.xml, just unused until now) for password-reset emails.
 * <p>
 * SMTP credentials ({@code MAIL_HOST}/{@code MAIL_USERNAME}/{@code MAIL_PASSWORD})
 * are optional env vars - see application.properties. If they're left unset, the
 * autoconfigured {@link JavaMailSender} bean still exists but {@link #send} throws
 * when it actually tries to connect; callers (see
 * {@code com.collegeerp.Backend.auth.PasswordResetService} and
 * {@code AuthController}) catch that and log the reset link instead, so
 * forgot-password stays usable in local/dev environments with no mail server
 * configured, while sending real email once SMTP is configured in production.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@college-erp.local}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink, long expiryMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your College ERP password");
        message.setText(
                "We received a request to reset your College ERP password.\n\n"
                        + "Click the link below to choose a new password. This link expires in "
                        + expiryMinutes + " minutes and can only be used once:\n\n"
                        + resetLink + "\n\n"
                        + "If you didn't request this, you can safely ignore this email - your "
                        + "password will not be changed."
        );
        mailSender.send(message);
    }
}
