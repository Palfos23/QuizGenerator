package com.quizapp.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromDisplayName;

    public MailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String fromAddress,
                        // Gmail SMTP requires the From *address* to be the authenticated
                        // account (or one of its verified "Send mail as" aliases) - it'll
                        // reject or silently rewrite anything else. This display name is
                        // the one part that's actually free to customize, so a reset/verify
                        // email reads "Quizzes <you@gmail.com>" rather than your own name.
                        @Value("${app.mail.from-name:Quizzes}") String fromDisplayName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromDisplayName = fromDisplayName;
    }

    /**
     * Deliberately swallows send failures rather than throwing - AuthService#requestPasswordReset
     * always returns the same generic "if that email exists..." response to the caller regardless
     * of whether sending actually succeeded, so a broken SMTP config or a Gmail hiccup shouldn't
     * turn into a 500 that also tips off a caller probing for which emails have accounts. A failure
     * here is still visible - just server-side, in the logs - not in the HTTP response.
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        send(toEmail, "Reset your Quizzes password",
                "Someone (hopefully you) asked to reset the password on your Quizzes account.\n\n" +
                "Reset it here - this link works for 30 minutes:\n" + resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email - your password won't change.");
    }

    /** Same swallow-and-log approach as sendPasswordResetEmail - see its comment. */
    public void sendVerificationEmail(String toEmail, String verifyLink) {
        send(toEmail, "Verify your Quizzes email",
                "Welcome to Quizzes! Confirm this is your email address to finish setting up your account.\n\n" +
                "Verify it here - this link works for 30 minutes:\n" + verifyLink + "\n\n" +
                "If you didn't create this account, you can safely ignore this email.");
    }

    private void send(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(new InternetAddress(fromAddress, fromDisplayName));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send '{}' email to {}: {}", subject, toEmail, e.getMessage());
        }
    }
}
