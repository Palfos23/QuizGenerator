package com.quizapp.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD);
    private static final Font QUESTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font ANSWER_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, new java.awt.Color(0, 110, 60));
    private static final Font BLANK_FONT = new Font(Font.HELVETICA, 11, Font.ITALIC, java.awt.Color.GRAY);
    private static final Font META_FONT = new Font(Font.HELVETICA, 9, Font.ITALIC, java.awt.Color.GRAY);

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
}
