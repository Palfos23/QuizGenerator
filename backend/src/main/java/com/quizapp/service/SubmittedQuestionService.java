package com.quizapp.service;

import com.quizapp.dto.RejectSubmissionRequest;
import com.quizapp.dto.SubmittedQuestionDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.Question;
import com.quizapp.model.SubmissionStatus;
import com.quizapp.model.SubmittedQuestion;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.SubmittedQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubmittedQuestionService {

    private final SubmittedQuestionRepository submittedQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AppUserRepository appUserRepository;

    public SubmittedQuestionService(SubmittedQuestionRepository submittedQuestionRepository,
                                     QuestionRepository questionRepository,
                                     AppUserRepository appUserRepository) {
        this.submittedQuestionRepository = submittedQuestionRepository;
        this.questionRepository = questionRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public SubmittedQuestionDto submit(String userEmail, SubmittedQuestionDto dto) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for " + userEmail));

        SubmittedQuestion entity = new SubmittedQuestion();
        entity.setQuestionText(dto.getQuestionText());
        entity.setCategory(dto.getCategory());
        entity.setDifficultyLevel(dto.getDifficultyLevel());
        entity.setLanguage(dto.getLanguage());
        entity.setAnswer(dto.getAnswer());
        entity.setCouldChange(dto.isCouldChange());
        entity.setSubmittedBy(user);
        entity.setStatus(SubmissionStatus.PENDING);

        return toDto(submittedQuestionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<SubmittedQuestionDto> listMine(String userEmail) {
        return submittedQuestionRepository.findBySubmittedBy_EmailOrderByCreatedAtDesc(userEmail)
                .stream().map(SubmittedQuestionService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubmittedQuestionDto> listAll() {
        return submittedQuestionRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(SubmittedQuestionService::toDto).collect(Collectors.toList());
    }

    /** Approving copies the submission into the shared question bank, available to everyone. */
    @Transactional
    public SubmittedQuestionDto approve(Long id) {
        SubmittedQuestion submission = submittedQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No submission found with id " + id));

        Question question = new Question();
        question.setQuestionText(submission.getQuestionText());
        question.setCategory(submission.getCategory());
        question.setDifficultyLevel(submission.getDifficultyLevel());
        question.setLanguage(submission.getLanguage());
        question.setAnswer(submission.getAnswer());
        question.setCouldChange(submission.isCouldChange());
        questionRepository.save(question);

        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setRejectionReason(null);
        return toDto(submittedQuestionRepository.save(submission));
    }

    /**
     * Rejecting keeps the submission exactly where it is - it never gets copied into
     * the shared bank, but the submitter can still use it personally (see
     * QuizService's includeMySubmissions handling), and can see the reason here.
     */
    @Transactional
    public SubmittedQuestionDto reject(Long id, RejectSubmissionRequest request) {
        SubmittedQuestion submission = submittedQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No submission found with id " + id));
        submission.setStatus(SubmissionStatus.REJECTED);
        submission.setRejectionReason(request.getReason());
        return toDto(submittedQuestionRepository.save(submission));
    }

    static SubmittedQuestionDto toDto(SubmittedQuestion s) {
        SubmittedQuestionDto dto = new SubmittedQuestionDto();
        dto.setId(s.getId());
        dto.setQuestionText(s.getQuestionText());
        dto.setCategory(s.getCategory());
        dto.setDifficultyLevel(s.getDifficultyLevel());
        dto.setLanguage(s.getLanguage());
        dto.setAnswer(s.getAnswer());
        dto.setCouldChange(s.isCouldChange());
        dto.setStatus(s.getStatus());
        dto.setRejectionReason(s.getRejectionReason());
        dto.setSubmitterName(s.getSubmittedBy().getName());
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }
}
