package com.quizapp.service;

import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import com.quizapp.dto.SavedQuizSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.SavedQuiz;
import com.quizapp.model.SavedQuizQuestion;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.SavedQuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class SavedQuizService {

    private final SavedQuizRepository savedQuizRepository;
    private final AppUserRepository appUserRepository;

    public SavedQuizService(SavedQuizRepository savedQuizRepository, AppUserRepository appUserRepository) {
        this.savedQuizRepository = savedQuizRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public QuizDto save(String ownerEmail, QuizDto quiz) {
        AppUser owner = appUserRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for " + ownerEmail));

        SavedQuiz entity = new SavedQuiz();
        entity.setOwner(owner);
        entity.setTitle(titleOrDefault(quiz));
        entity.setLanguage(quiz.getLanguage());
        entity.setQuestions(buildSnapshot(quiz));

        SavedQuiz saved = savedQuizRepository.save(entity);
        return toDto(saved);
    }

    /** Overwrites an existing saved quiz's title/language/questions in place - same id, same createdAt. */
    @Transactional
    public QuizDto update(String ownerEmail, Long id, QuizDto quiz) {
        SavedQuiz existing = savedQuizRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No saved quiz found with id " + id));

        existing.setTitle(titleOrDefault(quiz));
        existing.setLanguage(quiz.getLanguage());
        existing.setQuestions(buildSnapshot(quiz));

        SavedQuiz saved = savedQuizRepository.save(existing);
        return toDto(saved);
    }

    private String titleOrDefault(QuizDto quiz) {
        return quiz.getTitle() == null || quiz.getTitle().isBlank() ? "My Quiz" : quiz.getTitle();
    }

    private List<SavedQuizQuestion> buildSnapshot(QuizDto quiz) {
        return IntStream.range(0, quiz.getQuestions().size())
                .mapToObj(i -> {
                    QuestionDto q = quiz.getQuestions().get(i);
                    SavedQuizQuestion sq = new SavedQuizQuestion();
                    sq.setOrderIndex(i);
                    sq.setQuestionText(q.getQuestionText());
                    sq.setCategory(q.getCategory());
                    sq.setDifficultyLevel(q.getDifficultyLevel());
                    sq.setAnswer(q.getAnswer());
                    return sq;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SavedQuizSummaryDto> listForUser(String ownerEmail) {
        return savedQuizRepository.findByOwner_EmailOrderByCreatedAtDesc(ownerEmail).stream()
                .map(q -> new SavedQuizSummaryDto(q.getId(), q.getTitle(), q.getLanguage(),
                        q.getQuestions().size(), q.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizDto getOne(String ownerEmail, Long id) {
        SavedQuiz quiz = savedQuizRepository.findByIdAndOwner_Email(id, ownerEmail)
                // Same message/status whether the quiz doesn't exist or belongs to someone else -
                // don't give an attacker a way to distinguish "not yours" from "doesn't exist".
                .orElseThrow(() -> new ResourceNotFoundException("No saved quiz found with id " + id));
        return toDto(quiz);
    }

    @Transactional
    public void delete(String ownerEmail, Long id) {
        SavedQuiz quiz = savedQuizRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No saved quiz found with id " + id));
        savedQuizRepository.delete(quiz);
    }

    static QuizDto toDto(SavedQuiz entity) {
        QuizDto dto = new QuizDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setLanguage(entity.getLanguage());
        dto.setQuestions(entity.getQuestions().stream()
                .map(sq -> {
                    QuestionDto q = new QuestionDto();
                    q.setQuestionText(sq.getQuestionText());
                    q.setCategory(sq.getCategory());
                    q.setDifficultyLevel(sq.getDifficultyLevel());
                    q.setLanguage(entity.getLanguage());
                    q.setAnswer(sq.getAnswer());
                    return q;
                })
                .collect(Collectors.toList()));
        return dto;
    }
}
