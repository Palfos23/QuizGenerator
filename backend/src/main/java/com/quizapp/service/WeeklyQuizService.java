package com.quizapp.service;

import com.quizapp.dto.WeeklyQuizPlayStateDto;
import com.quizapp.dto.WeeklyQuizResultDto;
import com.quizapp.dto.WeeklyQuizSubmitRequest;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.WeeklyQuizAnswerRepository;
import com.quizapp.repository.WeeklyQuizAttemptRepository;
import com.quizapp.repository.WeeklyQuizSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// A pub-quiz-style solo mode: 15 random questions a week, free-text answers.
// Unlike Grid/Lineup (admin hand-curates each board), the week's question set
// is generated automatically the first time anyone asks for it - no admin
// authoring step, no scheduled job. See GridPlayService's identical
// lazy-create-on-first-request philosophy for findOrCreateAttempt/isActive.
@Service
public class WeeklyQuizService {

    private static final int QUESTIONS_PER_WEEK = 15;
    // How many previous weeks' questions to exclude when generating a new
    // set - a cheap safety net against repeats. The bank is in the thousands
    // (Norwegian-only for now), so this rarely even matters, but costs one
    // query either way.
    private static final int AVOID_REPEATS_WEEKS = 8;

    private final WeeklyQuizSetRepository setRepository;
    private final WeeklyQuizAttemptRepository attemptRepository;
    private final WeeklyQuizAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AppUserRepository appUserRepository;

    public WeeklyQuizService(WeeklyQuizSetRepository setRepository,
                              WeeklyQuizAttemptRepository attemptRepository,
                              WeeklyQuizAnswerRepository answerRepository,
                              QuestionRepository questionRepository,
                              AppUserRepository appUserRepository) {
        this.setRepository = setRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public WeeklyQuizSet getOrCreateCurrentSet() {
        LocalDate weekStart = currentWeekStart();
        return setRepository.findByWeekStartDate(weekStart).orElseGet(() -> generateSet(weekStart));
    }

    private LocalDate currentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // Package-private (not private) so WeeklyQuizServiceTest can exercise
    // generation for an arbitrary week directly - calling it through
    // getOrCreateCurrentSet() would only ever hit "this week", which every
    // other test in the class has already raced to create first. Needs its
    // own @Transactional for that direct-call path too (getQuestionIds() on
    // the "recently used" sets below is a lazy collection).
    @Transactional
    WeeklyQuizSet generateSet(LocalDate weekStart) {
        // Norwegian-only for now, same one-language-at-a-time approach as the
        // birthday quiz's Norway-specific content - revisit if a
        // multi-language weekly quiz is ever wanted.
        List<Question> candidates = new ArrayList<>(questionRepository.findByLanguage(Language.NO));

        Set<Long> recentlyUsed = setRepository.findByWeekStartDateAfter(weekStart.minusWeeks(AVOID_REPEATS_WEEKS))
                .stream()
                .flatMap(s -> s.getQuestionIds().stream())
                .collect(Collectors.toSet());
        candidates.removeIf(q -> recentlyUsed.contains(q.getId()));

        Collections.shuffle(candidates);
        int wanted = Math.min(QUESTIONS_PER_WEEK, candidates.size());

        WeeklyQuizSet set = new WeeklyQuizSet();
        set.setWeekStartDate(weekStart);
        set.setQuestionIds(candidates.stream().limit(wanted).map(Question::getId).collect(Collectors.toList()));
        return setRepository.save(set);
    }

    @Transactional
    public WeeklyQuizPlayStateDto getPlayState(String userEmail) {
        WeeklyQuizSet set = getOrCreateCurrentSet();
        WeeklyQuizAttempt attempt = findOrCreateAttempt(set, userEmail);

        WeeklyQuizPlayStateDto dto = new WeeklyQuizPlayStateDto();
        dto.setWeekStartDate(set.getWeekStartDate());
        dto.setAttemptStatus(attempt.getStatus().name());

        List<Question> questions = questionRepository.findAllById(set.getQuestionIds());
        java.util.Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        List<WeeklyQuizPlayStateDto.QuestionDto> questionDtos = new ArrayList<>();
        for (int i = 0; i < set.getQuestionIds().size(); i++) {
            Question q = byId.get(set.getQuestionIds().get(i));
            if (q != null) {
                questionDtos.add(new WeeklyQuizPlayStateDto.QuestionDto(i + 1, q.getId(), q.getQuestionText()));
            }
        }
        dto.setQuestions(questionDtos);

        if (attempt.getStatus() == WeeklyQuizAttemptStatus.GRADED) {
            dto.setResult(buildResult(attempt));
        }
        return dto;
    }

    private WeeklyQuizResultDto buildResult(WeeklyQuizAttempt attempt) {
        List<WeeklyQuizAnswer> answers = answerRepository.findByAttempt_Id(attempt.getId());
        WeeklyQuizResultDto result = new WeeklyQuizResultDto();
        result.setScore(attempt.getScore());
        result.setMaxScore(answers.size());
        List<WeeklyQuizResultDto.AnswerResultDto> rows = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            WeeklyQuizAnswer a = answers.get(i);
            rows.add(new WeeklyQuizResultDto.AnswerResultDto(
                    i + 1, a.getQuestion().getQuestionText(), a.getAnswerText(),
                    a.getQuestion().getAnswer(), a.getVerdict().name()));
        }
        result.setAnswers(rows);
        return result;
    }

    @Transactional
    public WeeklyQuizPlayStateDto submitAnswers(String userEmail, WeeklyQuizSubmitRequest request) {
        WeeklyQuizSet set = getOrCreateCurrentSet();
        WeeklyQuizAttempt attempt = findOrCreateAttempt(set, userEmail);
        if (attempt.getStatus() != WeeklyQuizAttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("You've already submitted this week's quiz.");
        }

        List<Question> questions = questionRepository.findAllById(set.getQuestionIds());
        java.util.Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        java.util.Map<Long, String> submittedByQuestionId = (request.getAnswers() == null ? List.<WeeklyQuizSubmitRequest.AnswerSubmission>of() : request.getAnswers())
                .stream()
                .collect(Collectors.toMap(WeeklyQuizSubmitRequest.AnswerSubmission::getQuestionId,
                        a -> a.getAnswerText() == null ? "" : a.getAnswerText(), (a, b) -> a));

        boolean anyPending = false;
        for (Long questionId : set.getQuestionIds()) {
            Question question = byId.get(questionId);
            if (question == null) continue; // question deleted since the set was generated - skip rather than fail the whole submission
            String raw = submittedByQuestionId.getOrDefault(questionId, "");
            String trimmed = raw.trim();

            WeeklyQuizAnswer answer = new WeeklyQuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setAnswerText(trimmed);
            if (trimmed.isEmpty()) {
                answer.setVerdict(WeeklyQuizAnswerVerdict.INCORRECT);
            } else if (trimmed.equalsIgnoreCase(question.getAnswer().trim())) {
                answer.setVerdict(WeeklyQuizAnswerVerdict.CORRECT);
            } else {
                answer.setVerdict(WeeklyQuizAnswerVerdict.PENDING);
                anyPending = true;
            }
            answerRepository.save(answer);
        }

        attempt.setSubmittedAt(Instant.now());
        if (anyPending) {
            attempt.setStatus(WeeklyQuizAttemptStatus.SUBMITTED);
        } else {
            gradeAttempt(attempt);
        }
        attemptRepository.save(attempt);

        return getPlayState(userEmail);
    }

    // Called both right after submit (when nothing needs review) and by
    // WeeklyQuizReviewService.resolve (once the last pending answer for this
    // attempt is resolved).
    void gradeAttempt(WeeklyQuizAttempt attempt) {
        long correctCount = answerRepository.countByAttempt_IdAndVerdict(attempt.getId(), WeeklyQuizAnswerVerdict.CORRECT);
        attempt.setScore((int) correctCount);
        attempt.setStatus(WeeklyQuizAttemptStatus.GRADED);
    }

    private WeeklyQuizAttempt findOrCreateAttempt(WeeklyQuizSet set, String userEmail) {
        return attemptRepository.findBySet_IdAndUser_Email(set.getId(), userEmail)
                .orElseGet(() -> {
                    AppUser owner = appUserRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new ResourceNotFoundException("No account found for " + userEmail));
                    WeeklyQuizAttempt fresh = new WeeklyQuizAttempt();
                    fresh.setSet(set);
                    fresh.setUser(owner);
                    return attemptRepository.save(fresh);
                });
    }
}
