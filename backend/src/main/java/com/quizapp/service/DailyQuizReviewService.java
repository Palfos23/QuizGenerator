package com.quizapp.service;

import com.quizapp.dto.DailyQuizAttemptDetailDto;
import com.quizapp.dto.DailyQuizDayAttemptsDto;
import com.quizapp.dto.DailyQuizPendingAttemptDto;
import com.quizapp.dto.QuestionDto;
import com.quizapp.model.Question;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.DailyQuizAnswer;
import com.quizapp.model.DailyQuizAnswerVerdict;
import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizAttemptStatus;
import com.quizapp.model.DailyQuizSet;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Admin-facing grading queue for whichever Daily Quiz answers weren't an
// exact match (see DailyQuizService.submitAnswers) - mirrors
// SubmittedQuestionService's PENDING-queue pattern. Grouped by attempt (one
// player's one day) rather than a single flat list of answers, so it's
// always clear whose answers are being corrected - see listPendingAttempts.
@Service
public class DailyQuizReviewService {

    private final DailyQuizAnswerRepository answerRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final DailyQuizSetRepository setRepository;
    private final DailyQuizService dailyQuizService;
    private final QuestionRepository questionRepository;

    public DailyQuizReviewService(DailyQuizAnswerRepository answerRepository,
                                   DailyQuizAttemptRepository attemptRepository,
                                   DailyQuizSetRepository setRepository,
                                   DailyQuizService dailyQuizService,
                                   QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.setRepository = setRepository;
        this.dailyQuizService = dailyQuizService;
        this.questionRepository = questionRepository;
    }

    // The day's questions in the order players see them, with their stored answers - so an admin can
    // spot and fix a wrong question or answer without hunting for it in the question bank. A question
    // that has since been deleted from the bank is simply left out.
    @Transactional(readOnly = true)
    public List<QuestionDto> listQuestionsForSet(Long setId) {
        DailyQuizSet set = setRepository.findById(setId)
                .filter(found -> !DailyQuizService.isExpired(found.getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("No daily quiz found with id " + setId));
        Map<Long, Question> byId = questionRepository.findAllById(set.getQuestionIds()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        return set.getQuestionIds().stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(com.quizapp.service.QuestionMapper::toDto)
                .collect(Collectors.toList());
    }

    // One row per player+day that still has at least one PENDING answer -
    // disappears once every answer in that attempt has been resolved.
    @Transactional(readOnly = true)
    public List<DailyQuizPendingAttemptDto> listPendingAttempts() {
        Map<Long, List<DailyQuizAnswer>> byAttemptId = answerRepository.findByVerdict(DailyQuizAnswerVerdict.PENDING).stream()
                // A quiz that has reached its maximum age is gone for admins too, not just players -
                // even in the gap before the hourly cleanup actually deletes it.
                .filter(a -> !DailyQuizService.isExpired(a.getAttempt().getSet().getQuizDate()))
                .collect(Collectors.groupingBy(a -> a.getAttempt().getId()));

        return byAttemptId.values().stream()
                .map(answers -> {
                    DailyQuizAttempt attempt = answers.get(0).getAttempt();
                    return new DailyQuizPendingAttemptDto(
                            attempt.getId(), attempt.getUser().getName(), attempt.getSet().getQuizDate(), answers.size());
                })
                .sorted(Comparator.comparing(DailyQuizPendingAttemptDto::getQuizDate).reversed()
                        .thenComparing(DailyQuizPendingAttemptDto::getPlayerName))
                .collect(Collectors.toList());
    }

    // Every answer for one player's one attempt, not just the pending ones -
    // gives the admin the full context (what they already got right/wrong)
    // alongside the handful still needing a decision.
    @Transactional(readOnly = true)
    public DailyQuizAttemptDetailDto getAttemptDetail(Long attemptId) {
        DailyQuizAttempt attempt = attemptRepository.findById(attemptId)
                .filter(a -> !DailyQuizService.isExpired(a.getSet().getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found with id " + attemptId));
        List<DailyQuizAnswer> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attemptId);

        List<DailyQuizAttemptDetailDto.AnswerDto> rows = new java.util.ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            DailyQuizAnswer a = answers.get(i);
            rows.add(new DailyQuizAttemptDetailDto.AnswerDto(
                    a.getId(), i + 1, a.getQuestion().getQuestionText(), a.getAnswerText(),
                    a.getQuestion().getAnswer(), a.getVerdict().name(), a.getQuestion().getPhotoUrl(),
                    DailyQuizScoring.isScoredAsYear(a.getQuestion()), a.getPoints(), isReviewable(a)));
        }
        int maxScore = answers.stream().mapToInt(a -> DailyQuizScoring.maxPoints(a.getQuestion())).sum();
        boolean graded = attempt.getStatus() == DailyQuizAttemptStatus.GRADED;
        return new DailyQuizAttemptDetailDto(attempt.getId(), attempt.getSet().getId(), attempt.getUser().getName(),
                attempt.getSet().getQuizDate(), attempt.getStatus().name(), graded ? attempt.getScore() : null, maxScore, rows);
    }

    // Everyone who submitted this day's quiz, graded or not - the way back to an attempt whose
    // answers have all been decided (so it's gone from the waiting list) when a decision needs fixing.
    @Transactional(readOnly = true)
    public DailyQuizDayAttemptsDto listAttemptsForSet(Long setId) {
        DailyQuizSet set = setRepository.findById(setId)
                .filter(found -> !DailyQuizService.isExpired(found.getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("No daily quiz found with id " + setId));
        List<DailyQuizAttempt> attempts = attemptRepository.findBySet_Id(setId).stream()
                .filter(a -> a.getStatus() != DailyQuizAttemptStatus.IN_PROGRESS)
                .collect(Collectors.toList());
        List<Long> attemptIds = attempts.stream().map(DailyQuizAttempt::getId).collect(Collectors.toList());
        Map<Long, List<DailyQuizAnswer>> answersByAttempt = attemptIds.isEmpty()
                ? Map.of()
                : answerRepository.findByAttempt_IdIn(attemptIds).stream()
                        .collect(Collectors.groupingBy(a -> a.getAttempt().getId()));

        List<DailyQuizDayAttemptsDto.AttemptRow> rows = attempts.stream().map(a -> {
            List<DailyQuizAnswer> answers = answersByAttempt.getOrDefault(a.getId(), List.of());
            int pending = (int) answers.stream().filter(x -> x.getVerdict() == DailyQuizAnswerVerdict.PENDING).count();
            int max = answers.stream().mapToInt(x -> DailyQuizScoring.maxPoints(x.getQuestion())).sum();
            boolean graded = a.getStatus() == DailyQuizAttemptStatus.GRADED;
            return new DailyQuizDayAttemptsDto.AttemptRow(
                    a.getId(), a.getUser().getName(), a.getStatus().name(), graded ? a.getScore() : null, max, pending);
        }).sorted(Comparator.comparing(DailyQuizDayAttemptsDto.AttemptRow::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        return new DailyQuizDayAttemptsDto(set.getQuizDate(), rows);
    }

    /**
     * Whether an admin can (re)decide this answer. Not the ones that are never a judgement call:
     * a Year question (graded by arithmetic), a blank answer (nothing to judge), or an exact match to
     * the stored answer (can't be wrong). Everything else - including answers an admin already
     * decided - can be changed, so a mis-click is fixable.
     */
    static boolean isReviewable(DailyQuizAnswer answer) {
        String given = answer.getAnswerText();
        if (given == null || given.isBlank()) return false;
        if (DailyQuizScoring.isScoredAsYear(answer.getQuestion())) return false;
        return !given.trim().equalsIgnoreCase(answer.getQuestion().getAnswer().trim());
    }

    /**
     * Decides an answer - or changes an earlier decision. Re-marking an already-decided answer
     * is deliberately allowed (a mis-click must be fixable): if that attempt was already graded,
     * its score is recalculated straight away, so the player's result and the leaderboard follow.
     */
    @Transactional
    public void resolve(Long answerId, boolean correct) {
        DailyQuizAnswer answer = answerRepository.findById(answerId)
                .filter(a -> !DailyQuizService.isExpired(a.getAttempt().getSet().getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("No answer found with id " + answerId));
        if (!isReviewable(answer)) {
            throw new IllegalStateException("This answer is graded automatically and can't be changed.");
        }
        DailyQuizAnswerVerdict decision = correct ? DailyQuizAnswerVerdict.CORRECT : DailyQuizAnswerVerdict.INCORRECT;
        if (answer.getVerdict() == decision) {
            return; // already decided that way (a double click, or a page that was a little stale)
        }
        answer.setVerdict(decision);
        answer.setPoints(1); // an ordinary question is worth 1 when correct
        answerRepository.save(answer);

        // Nothing left waiting: grade the attempt - for the first time, or again if this was a change
        // to one that was already graded.
        long stillPending = answerRepository.countByAttempt_IdAndVerdict(
                answer.getAttempt().getId(), DailyQuizAnswerVerdict.PENDING);
        if (stillPending == 0) {
            var attempt = answer.getAttempt();
            dailyQuizService.gradeAttempt(attempt);
            attemptRepository.save(attempt);
        }
    }
}
