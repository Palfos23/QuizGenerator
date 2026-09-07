package com.quizapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Deliberately swallows send failures rather than throwing - AuthService#requestPasswordReset
     * always returns the same generic "if that email exists..." response to the caller regardless
     * of whether sending actually succeeded, so a broken SMTP config or a Gmail hiccup shouldn't
     * turn into a 500 that also tips off a caller probing for which emails have accounts. A failure
     * here is still visible - just server-side, in the logs - not in the HTTP response.
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your Quizzes password");
        message.setText(
                "Someone (hopefully you) asked to reset the password on your Quizzes account.\n\n" +
                "Reset it here - this link works for 30 minutes:\n" + resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email - your password won't change."
        );
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
