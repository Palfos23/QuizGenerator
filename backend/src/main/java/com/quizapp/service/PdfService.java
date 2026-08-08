package com.quizapp.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URL;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD);
    private static final Font QUESTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font ANSWER_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, new java.awt.Color(0, 110, 60));
    private static final Font BLANK_FONT = new Font(Font.HELVETICA, 11, Font.ITALIC, java.awt.Color.GRAY);
    private static final Font META_FONT = new Font(Font.HELVETICA, 9, Font.ITALIC, java.awt.Color.GRAY);

    // Cap the printed size so one huge remote photo can't blow out the page
    // layout - fits within a modest box while keeping its own aspect ratio.
    private static final float MAX_IMAGE_WIDTH = 220f;
    private static final float MAX_IMAGE_HEIGHT = 220f;

    /**
     * Renders the quiz as a PDF. When includeAnswers is false, a blank answer
     * line is printed instead - useful for a "print for guests" version.
     */
    public byte[] renderQuiz(QuizDto quiz, boolean includeAnswers) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph(quiz.getTitle(), TITLE_FONT));
            document.add(Chunk.NEWLINE);

            int number = 1;
            for (QuestionDto question : quiz.getQuestions()) {
                document.add(new Paragraph(number + ". " + question.getQuestionText(), QUESTION_FONT));
                document.add(new Paragraph(
                        question.getCategory() + " - Difficulty " + question.getDifficultyLevel() + "/10", META_FONT));

                if (question.getPhotoUrl() != null && !question.getPhotoUrl().isBlank()) {
                    addPhoto(document, question.getPhotoUrl());
                }

                if (includeAnswers) {
                    document.add(new Paragraph("Answer: " + question.getAnswer(), ANSWER_FONT));
                } else {
                    document.add(new Paragraph("Answer: _______________________________", BLANK_FONT));
                }

                document.add(Chunk.NEWLINE);
                number++;
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // Fetches and embeds a question's photo. Deliberately swallows failures
    // (dead link, host down, not actually an image) rather than letting one
    // bad URL crash the whole quiz download - the question text and answer
    // still print fine either way, just without the picture.
    private void addPhoto(Document document, String photoUrl) {
        try {
            Image image = Image.getInstance(new URL(photoUrl));
            image.scaleToFit(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
            image.setSpacingBefore(6f);
            image.setSpacingAfter(6f);
            document.add(image);
        } catch (Exception e) {
            log.warn("Could not embed question photo '{}' in PDF: {}", photoUrl, e.getMessage());
        }
    }
}
