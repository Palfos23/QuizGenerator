package com.quizapp.service;

import com.quizapp.dto.ImportResultDto;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.QuestionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Expects a CSV with a header row containing (case-insensitive, any order):
 * question, category, difficulty, language, answer, and optionally couldChange.
 *
 * Example:
 *   question,category,difficulty,language,answer,couldChange
 *   "What is the capital of Italy?",Geography,2,EN,Rome,false
 */
@Service
public class QuestionImportService {

    private static final Set<String> TRUE_VALUES = Set.of("true", "yes", "1", "y");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final QuestionRepository questionRepository;

    public QuestionImportService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional
    public ImportResultDto importCsv(MultipartFile file) {
        ImportResultDto result = new ImportResultDto();
        List<Question> toSave = new ArrayList<>();

        try (Reader reader = openStrippingBom(file)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = new CSVParser(reader, format)) {
                for (CSVRecord record : parser) {
                    try {
                        toSave.add(parseRow(record));
                    } catch (RowValidationException e) {
                        result.setSkipped(result.getSkipped() + 1);
                        result.getErrors().add("Row " + record.getRecordNumber() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // malformed CSV structure itself (e.g. no header row) - not a per-row problem
            throw new IllegalArgumentException("Could not parse the CSV: " + e.getMessage());
        }

        questionRepository.saveAll(toSave);
        result.setImported(toSave.size());
        return result;
    }

    /**
     * Excel and Google Sheets' "CSV UTF-8" export prepends a byte-order-mark (EF BB BF).
     * Left in place, it silently attaches itself to the first header's name (turning
     * "question" into "\uFEFFquestion"), which made every row fail with a false
     * "question is required" error. Strip it here if present.
     */
    private Reader openStrippingBom(MultipartFile file) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(
                new BufferedInputStream(file.getInputStream()), UTF8_BOM.length);

        byte[] maybeBom = new byte[UTF8_BOM.length];
        int read = pushback.read(maybeBom, 0, UTF8_BOM.length);

        if (read < UTF8_BOM.length || !java.util.Arrays.equals(maybeBom, UTF8_BOM)) {
            // not a BOM (or a short file) - put back whatever we peeked at
            if (read > 0) {
                pushback.unread(maybeBom, 0, read);
            }
        }

        return new InputStreamReader(pushback, StandardCharsets.UTF_8);
    }

    private Question parseRow(CSVRecord record) {
        String questionText = safeGet(record, "question");
        String category = safeGet(record, "category");
        String difficultyRaw = safeGet(record, "difficulty");
        String languageRaw = safeGet(record, "language");
        String answer = safeGet(record, "answer");
        String couldChangeRaw = safeGet(record, "couldChange");

        if (isBlank(questionText)) throw new RowValidationException("\"question\" is required");
        if (isBlank(category)) throw new RowValidationException("\"category\" is required");
        if (isBlank(answer)) throw new RowValidationException("\"answer\" is required");
        if (isBlank(languageRaw)) throw new RowValidationException("\"language\" is required");
        if (isBlank(difficultyRaw)) throw new RowValidationException("\"difficulty\" is required");

        int difficulty;
        try {
            difficulty = Integer.parseInt(difficultyRaw.trim());
        } catch (NumberFormatException e) {
            throw new RowValidationException("\"difficulty\" must be a whole number, got \"" + difficultyRaw + "\"");
        }
        if (difficulty < 1 || difficulty > 10) {
            throw new RowValidationException("\"difficulty\" must be between 1 and 10, got " + difficulty);
        }

        Language language;
        try {
            language = Language.valueOf(languageRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RowValidationException("\"language\" must be one of EN, DE, FR, ES, NO - got \"" + languageRaw + "\"");
        }

        boolean couldChange = couldChangeRaw != null && TRUE_VALUES.contains(couldChangeRaw.trim().toLowerCase());

        Question question = new Question();
        question.setQuestionText(questionText.trim());
        question.setCategory(category.trim());
        question.setDifficultyLevel(difficulty);
        question.setLanguage(language);
        question.setAnswer(answer.trim());
        question.setCouldChange(couldChange);
        return question;
    }

    private String safeGet(CSVRecord record, String column) {
        return record.isMapped(column) ? record.get(column) : null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static final class RowValidationException extends RuntimeException {
        RowValidationException(String message) {
            super(message);
        }
    }
}
