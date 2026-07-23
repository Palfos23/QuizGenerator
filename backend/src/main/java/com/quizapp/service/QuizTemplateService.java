package com.quizapp.service;

import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import com.quizapp.dto.QuizTemplateSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.QuizTemplate;
import com.quizapp.model.QuizTemplateQuestion;
import com.quizapp.model.SavedQuiz;
import com.quizapp.model.SavedQuizQuestion;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.QuizTemplateRepository;
import com.quizapp.repository.SavedQuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class QuizTemplateService {

    private final QuizTemplateRepository quizTemplateRepository;
    private final SavedQuizRepository savedQuizRepository;
    private final AppUserRepository appUserRepository;

    public QuizTemplateService(QuizTemplateRepository quizTemplateRepository, SavedQuizRepository savedQuizRepository,
                                AppUserRepository appUserRepository) {
        this.quizTemplateRepository = quizTemplateRepository;
        this.savedQuizRepository = savedQuizRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<QuizTemplateSummaryDto> findAll() {
        return quizTemplateRepository.findAll().stream()
                .map(t -> new QuizTemplateSummaryDto(t.getId(), t.getTitle(), t.getLanguage(),
                        t.getQuestions().size(), t.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizDto getOne(Long id) {
        return toDto(findTemplate(id));
    }

    @Transactional
    public QuizDto create(QuizDto quiz) {
        QuizTemplate template = new QuizTemplate();
        applyDto(template, quiz);
        return toDto(quizTemplateRepository.save(template));
    }

    @Transactional
    public QuizDto update(Long id, QuizDto quiz) {
        QuizTemplate template = findTemplate(id);
        applyDto(template, quiz);
        return toDto(quizTemplateRepository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        if (!quizTemplateRepository.existsById(id)) {
            throw new ResourceNotFoundException("No quiz template found with id " + id);
        }
        quizTemplateRepository.deleteById(id);
    }

    /**
     * Copies a template into a brand-new SavedQuiz owned by this user - completely
     * independent from that point on. Editing their copy doesn't touch the template,
     * and later admin edits to the template don't touch anyone's existing copies.
     */
    @Transactional
    public QuizDto copyToMyQuizzes(Long templateId, String userEmail) {
        QuizTemplate template = findTemplate(templateId);
        AppUser owner = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for " + userEmail));

        SavedQuiz saved = new SavedQuiz();
        saved.setOwner(owner);
        saved.setTitle(template.getTitle());
        saved.setLanguage(template.getLanguage());

        List<SavedQuizQuestion> questions = template.getQuestions().stream()
                .map(tq -> {
                    SavedQuizQuestion sq = new SavedQuizQuestion();
                    sq.setOrderIndex(tq.getOrderIndex());
                    sq.setQuestionText(tq.getQuestionText());
                    sq.setCategory(tq.getCategory());
                    sq.setDifficultyLevel(tq.getDifficultyLevel());
                    sq.setAnswer(tq.getAnswer());
                    return sq;
                })
                .collect(Collectors.toList());
        saved.setQuestions(questions);

        SavedQuiz persisted = savedQuizRepository.save(saved);
        return SavedQuizService.toDto(persisted);
    }

    private QuizTemplate findTemplate(Long id) {
        return quizTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No quiz template found with id " + id));
    }

    private void applyDto(QuizTemplate template, QuizDto quiz) {
        template.setTitle(quiz.getTitle() == null || quiz.getTitle().isBlank() ? "Untitled quiz" : quiz.getTitle());
        template.setLanguage(quiz.getLanguage());

        List<QuizTemplateQuestion> questions = IntStream.range(0, quiz.getQuestions().size())
                .mapToObj(i -> {
                    QuestionDto q = quiz.getQuestions().get(i);
                    QuizTemplateQuestion tq = new QuizTemplateQuestion();
                    tq.setOrderIndex(i);
                    tq.setQuestionText(q.getQuestionText());
                    tq.setCategory(q.getCategory());
                    tq.setDifficultyLevel(q.getDifficultyLevel());
                    tq.setAnswer(q.getAnswer());
                    return tq;
                })
                .collect(Collectors.toList());
        template.setQuestions(questions);
    }

    private QuizDto toDto(QuizTemplate template) {
        QuizDto dto = new QuizDto();
        dto.setId(template.getId());
        dto.setTitle(template.getTitle());
        dto.setLanguage(template.getLanguage());
        dto.setQuestions(template.getQuestions().stream().map(tq -> {
            QuestionDto q = new QuestionDto();
            q.setId(-tq.getId()); // negative id: same collision-avoidance trick as submitted questions
            q.setQuestionText(tq.getQuestionText());
            q.setCategory(tq.getCategory());
            q.setDifficultyLevel(tq.getDifficultyLevel());
            q.setAnswer(tq.getAnswer());
            return q;
        }).collect(Collectors.toList()));
        dto.setWarnings(new ArrayList<>());
        return dto;
    }
}
