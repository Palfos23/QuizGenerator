package com.quizapp.service;

import com.quizapp.dto.QuizDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final PdfService pdfService;

    @Value("${quiz.mail.from:no-reply@quizapp.local}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, PdfService pdfService) {
        this.mailSender = mailSender;
        this.pdfService = pdfService;
    }

    public void sendQuiz(String recipientEmail, QuizDto quiz, boolean includeAnswers) {
        byte[] pdfBytes = pdfService.renderQuiz(quiz, includeAnswers);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("Your quiz: " + quiz.getTitle());
            helper.setText(
                    "Hi,\n\nYour quiz \"" + quiz.getTitle() + "\" (" + quiz.getQuestions().size() +
                            " questions) is attached as a PDF.\n\nHave fun!\n", false);
            helper.addAttachment(sanitizeFileName(quiz.getTitle()) + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(pdfBytes));
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to build email: " + e.getMessage(), e);
        }

        mailSender.send(message);
    }

    private String sanitizeFileName(String title) {
        return title.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
