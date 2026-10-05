package com.quizapp.service;

import com.quizapp.dto.DailyQuizPlayStateDto;
import com.quizapp.dto.DailyQuizResultDto;
import com.quizapp.dto.DailyQuizScoreboardDto;
import com.quizapp.dto.DailyQuizScoreboardEntryDto;
import com.quizapp.dto.DailyQuizSetSummaryDto;
import com.quizapp.dto.DailyQuizSubmitRequest;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizSetRepository;
import com.quizapp.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

// A pub-quiz-style solo mode: 15 random questions a day, free-text answers.
// Unlike Grid/Lineup (admin hand-curates each board), the day's question set
// is generated automatically the first time anyone asks for it - no admin
// authoring step. See GridPlayService's identical lazy-create-on-first-request
// philosophy for findOrCreateAttempt/isActive, and its
// findActive/findArchive/getScoreboard for the list+leaderboard conventions
// this class mirrors. A set older than RETENTION_DAYS is deleted by
// deleteOldSets below, same @Scheduled pattern as RoomCleanupService.
//
// This feature used to be "Weekly Quiz" (one set per calendar week) before
// switching to a daily cadence - see DailyQuizSet's class comment for why the
// underlying tables/columns are still named "weekly_quiz_*"/"week_start_date"
// even though every Java/API-facing name here says "daily".
@Service
public class DailyQuizService {

    private static final int QUESTIONS_PER_DAY = 15;
    private static final String LOGO_CATEGORY = "Logo";
    // A quiz stops being reachable once it's this many days old - e.g. with
    // RETENTION_DAYS = 7, a quiz from exactly 7 days ago is still the oldest
    // one kept, and one from 8 days ago is gone (deleteOldSets below deletes
    // anything strictly older than "today minus RETENTION_DAYS").
    private static final int RETENTION_DAYS = 7;

    private static final Logger log = LoggerFactory.getLogger(DailyQuizService.class);

    private final DailyQuizSetRepository setRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final DailyQuizAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AppUserRepository appUserRepository;
    private final AdminNotificationService adminNotificationService;

    public DailyQuizService(DailyQuizSetRepository setRepository,
                             DailyQuizAttemptRepository attemptRepository,
                             DailyQuizAnswerRepository answerRepository,
                             QuestionRepository questionRepository,
                             AppUserRepository appUserRepository,
                             AdminNotificationService adminNotificationService) {
        this.setRepository = setRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.appUserRepository = appUserRepository;
        this.adminNotificationService = adminNotificationService;
    }

    @Transactional
    public DailyQuizSet getOrCreateCurrentSet() {
        LocalDate today = LocalDate.now();
        return setRepository.findByQuizDate(today).orElseGet(() -> generateSet(today));
    }

    // Package-private (not private) so DailyQuizServiceTest can exercise
    // generation for an arbitrary day directly - calling it through
    // getOrCreateCurrentSet() would only ever hit "today", which every other
    // test in the class has already raced to create first. Needs its own
    // @Transactional for that direct-call path too (getQuestionIds() on the
    // "recently used" sets below is a lazy collection).
    @Transactional
    DailyQuizSet generateSet(LocalDate quizDate) {
        // Norwegian-only for now, same one-language-at-a-time approach as the
        // birthday quiz's Norway-specific content - revisit if a
        // multi-language daily quiz is ever wanted.
        List<Question> candidates = new ArrayList<>(questionRepository.findByLanguage(Language.NO));

        // Excludes questions from every set still within the retention
        // window - which in practice is every set still in the database,
        // since deleteOldSets purges anything older. +1 so the oldest
        // still-retained day (age == RETENTION_DAYS, per deleteOldSets'
        // boundary below) is included in the exclusion, not just the ones
        // newer than it. No separate "how far back" knob needed the way the
        // weekly version had one.
        Set<Long> recentlyUsed = setRepository.findByQuizDateAfter(quizDate.minusDays(RETENTION_DAYS + 1L))
                .stream()
                .flatMap(s -> s.getQuestionIds().stream())
                .collect(Collectors.toSet());
        // The quiz always opens with exactly ONE picture question from the Logo
        // category - never more, never fewer - and the other 14 are drawn from
        // everything that isn't a Logo question at all. A Logo question without a
        // photo is useless here (the question text is just "which airline?"), so only
        // ones with a photo qualify as the opener.
        List<Question> logoPool = candidates.stream()
                .filter(DailyQuizService::isLogoQuestionWithPhoto)
                .collect(Collectors.toList());
        List<Question> freshLogos = logoPool.stream()
                .filter(q -> !recentlyUsed.contains(q.getId()))
                .collect(Collectors.toList());
        // If every Logo question has been used within the retention window, repeating
        // one beats a quiz with no picture round at all.
        List<Question> logoChoices = freshLogos.isEmpty() ? logoPool : freshLogos;
        Question logoQuestion = logoChoices.isEmpty() ? null : logoChoices.get(new Random().nextInt(logoChoices.size()));

        List<Question> others = candidates.stream()
                .filter(q -> !isLogoCategory(q))
                .filter(q -> !recentlyUsed.contains(q.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(others);

        List<Long> questionIds = new ArrayList<>();
        if (logoQuestion != null) {
            questionIds.add(logoQuestion.getId());
        }
        others.stream()
                .limit(QUESTIONS_PER_DAY - questionIds.size())
                .map(Question::getId)
                .forEach(questionIds::add);

        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(quizDate);
        set.setQuestionIds(questionIds);
        return setRepository.save(set);
    }

    private static boolean isLogoCategory(Question q) {
        return q.getCategory() != null && LOGO_CATEGORY.equalsIgnoreCase(q.getCategory().trim());
    }

    private static boolean isLogoQuestionWithPhoto(Question q) {
        return isLogoCategory(q) && q.getPhotoUrl() != null && !q.getPhotoUrl().isBlank();
    }

    // Runs hourly, same cadence as RoomCleanupService - deletes any quiz set
    // (and its attempts/answers) once it's more than RETENTION_DAYS days old.
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void deleteOldSets() {
        LocalDate cutoff = LocalDate.now().minusDays(RETENTION_DAYS);
        List<DailyQuizSet> stale = setRepository.findByQuizDateBefore(cutoff);
        if (stale.isEmpty()) {
            return;
        }
        List<Long> setIds = stale.stream().map(DailyQuizSet::getId).collect(Collectors.toList());
        List<DailyQuizAttempt> attempts = attemptRepository.findBySet_IdIn(setIds);
        List<Long> attemptIds = attempts.stream().map(DailyQuizAttempt::getId).collect(Collectors.toList());

        // Children before parents: answers -> attempts -> sets.
        if (!attemptIds.isEmpty()) {
            answerRepository.deleteByAttempt_IdIn(attemptIds);
        }
        attemptRepository.deleteAll(attempts);
        setRepository.deleteAll(stale);
        log.info("Daily quiz cleanup: removed {} stale quiz(zes) older than {} day(s)", stale.size(), RETENTION_DAYS);
    }

    // Always exactly one entry (today's set, lazily created) - unlike Grid,
    // where several different boards can be active at once, Daily Quiz only
    // ever has one set per day. Still returned as a list so the list-page
    // frontend can treat it the same way as Grid's active/archive split.
    @Transactional
    public List<DailyQuizSetSummaryDto> findActive(String userEmail) {
        DailyQuizSet current = getOrCreateCurrentSet();
        return toSummaries(List.of(current), userEmail);
    }

    @Transactional(readOnly = true)
    public List<DailyQuizSetSummaryDto> findArchive(String userEmail) {
        List<DailyQuizSet> pastSets = setRepository.findByQuizDateBeforeOrderByQuizDateDesc(LocalDate.now());
        return toSummaries(pastSets, userEmail);
    }

    private List<DailyQuizSetSummaryDto> toSummaries(List<DailyQuizSet> sets, String userEmail) {
        List<Long> setIds = sets.stream().map(DailyQuizSet::getId).collect(Collectors.toList());
        Map<Long, DailyQuizAttempt> attemptBySetId = setIds.isEmpty()
                ? Map.of()
                : attemptRepository.findBySet_IdInAndUser_Email(setIds, userEmail).stream()
                        .collect(Collectors.toMap(a -> a.getSet().getId(), a -> a));

        return sets.stream().map(set -> {
            DailyQuizAttempt attempt = attemptBySetId.get(set.getId());
            String status = attempt == null ? "NOT_STARTED" : attempt.getStatus().name();
            Integer score = attempt != null && attempt.getStatus() == DailyQuizAttemptStatus.GRADED ? attempt.getScore() : null;
            return new DailyQuizSetSummaryDto(set.getId(), set.getQuizDate(), set.getQuestionIds().size(), status, score);
        }).collect(Collectors.toList());
    }

    @Transactional
    public DailyQuizPlayStateDto getPlayState(Long setId, String userEmail) {
        DailyQuizSet set = requireSet(setId);
        DailyQuizAttempt attempt = findOrCreateAttempt(set, userEmail);
        return toPlayStateDto(set, attempt);
    }

    private DailyQuizSet requireSet(Long setId) {
        return setRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("No daily quiz found with id " + setId));
    }

    private DailyQuizPlayStateDto toPlayStateDto(DailyQuizSet set, DailyQuizAttempt attempt) {
        DailyQuizPlayStateDto dto = new DailyQuizPlayStateDto();
        dto.setSetId(set.getId());
        dto.setQuizDate(set.getQuizDate());
        dto.setAttemptStatus(attempt.getStatus().name());

        if (attempt.getStatus() == DailyQuizAttemptStatus.IN_PROGRESS) {
            List<Question> questions = questionRepository.findAllById(set.getQuestionIds());
            Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
            List<DailyQuizPlayStateDto.QuestionDto> questionDtos = new ArrayList<>();
            for (int i = 0; i < set.getQuestionIds().size(); i++) {
                Question q = byId.get(set.getQuestionIds().get(i));
                if (q != null) {
                    questionDtos.add(new DailyQuizPlayStateDto.QuestionDto(i + 1, q.getId(), q.getQuestionText(), q.getPhotoUrl()));
                }
            }
            dto.setQuestions(questionDtos);
        } else {
            // SUBMITTED or GRADED - your own answers are always visible, even
            // while some are still "Under review" (score itself stays hidden
            // until nothing is left pending - see buildAnswersView).
            dto.setResult(buildAnswersView(attempt));
        }
        return dto;
    }

    private DailyQuizResultDto buildAnswersView(DailyQuizAttempt attempt) {
        List<DailyQuizAnswer> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attempt.getId());
        DailyQuizResultDto result = new DailyQuizResultDto();
        result.setScore(attempt.getStatus() == DailyQuizAttemptStatus.GRADED ? attempt.getScore() : null);
        result.setMaxScore(answers.size());
        List<DailyQuizResultDto.AnswerResultDto> rows = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            DailyQuizAnswer a = answers.get(i);
            boolean pending = a.getVerdict() == DailyQuizAnswerVerdict.PENDING;
            rows.add(new DailyQuizResultDto.AnswerResultDto(
                    i + 1, a.getQuestion().getQuestionText(), a.getAnswerText(),
                    pending ? null : a.getQuestion().getAnswer(), a.getVerdict().name(), a.getQuestion().getPhotoUrl()));
        }
        result.setAnswers(rows);
        return result;
    }

    @Transactional
    public DailyQuizPlayStateDto submitAnswers(Long setId, String userEmail, DailyQuizSubmitRequest request) {
        DailyQuizSet set = requireSet(setId);
        DailyQuizAttempt attempt = findOrCreateAttempt(set, userEmail);
        if (attempt.getStatus() != DailyQuizAttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("You've already submitted this quiz.");
        }

        List<Question> questions = questionRepository.findAllById(set.getQuestionIds());
        Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, String> submittedByQuestionId = (request.getAnswers() == null ? List.<DailyQuizSubmitRequest.AnswerSubmission>of() : request.getAnswers())
                .stream()
                .collect(Collectors.toMap(DailyQuizSubmitRequest.AnswerSubmission::getQuestionId,
                        a -> a.getAnswerText() == null ? "" : a.getAnswerText(), (a, b) -> a));

        int pendingCount = 0;
        for (Long questionId : set.getQuestionIds()) {
            Question question = byId.get(questionId);
            if (question == null) continue; // question deleted since the set was generated - skip rather than fail the whole submission
            String raw = submittedByQuestionId.getOrDefault(questionId, "");
            String trimmed = raw.trim();

            DailyQuizAnswer answer = new DailyQuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setAnswerText(trimmed);
            if (trimmed.isEmpty()) {
                answer.setVerdict(DailyQuizAnswerVerdict.INCORRECT);
            } else if (trimmed.equalsIgnoreCase(question.getAnswer().trim())) {
                answer.setVerdict(DailyQuizAnswerVerdict.CORRECT);
            } else {
                answer.setVerdict(DailyQuizAnswerVerdict.PENDING);
                pendingCount++;
            }
            answerRepository.save(answer);
        }

        attempt.setSubmittedAt(java.time.Instant.now());
        if (pendingCount > 0) {
            attempt.setStatus(DailyQuizAttemptStatus.SUBMITTED);
            adminNotificationService.notifyAdmin(
                    "Daily quiz needs review",
                    attempt.getUser().getName() + " submitted the daily quiz - "
                            + pendingCount + " answer" + (pendingCount == 1 ? "" : "s") + " to review.",
                    "/admin/daily-quiz-review");
        } else {
            gradeAttempt(attempt);
        }
        attemptRepository.save(attempt);

        return getPlayState(setId, userEmail);
    }

    // Called both right after submit (when nothing needs review) and by
    // DailyQuizReviewService.resolve (once the last pending answer for this
    // attempt is resolved).
    void gradeAttempt(DailyQuizAttempt attempt) {
        long correctCount = answerRepository.countByAttempt_IdAndVerdict(attempt.getId(), DailyQuizAnswerVerdict.CORRECT);
        attempt.setScore((int) correctCount);
        attempt.setStatus(DailyQuizAttemptStatus.GRADED);
    }

    @Transactional
    public void setLeaderboardPreference(Long setId, String userEmail, boolean includeOnLeaderboard) {
        DailyQuizAttempt attempt = attemptRepository.findBySet_IdAndUser_Email(setId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found for this daily quiz."));
        attempt.setIncludeOnLeaderboard(includeOnLeaderboard);
        attemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public DailyQuizScoreboardDto getScoreboard(Long setId, String requestingUserEmail) {
        requireSet(setId); // 404s if the set doesn't exist at all

        List<DailyQuizAttempt> gradedAttempts = attemptRepository.findBySet_Id(setId).stream()
                .filter(a -> a.getStatus() == DailyQuizAttemptStatus.GRADED)
                .collect(Collectors.toList());

        List<DailyQuizScoreboardEntryDto> entries = gradedAttempts.stream()
                // Opted-out players still count toward the average (just a number,
                // doesn't reveal who they are) but are excluded from the visible
                // ranked list - except your own row, which you can always see.
                .filter(a -> a.isIncludeOnLeaderboard() || a.getUser().getEmail().equals(requestingUserEmail))
                .map(a -> new DailyQuizScoreboardEntryDto(
                        a.getUser().getName(), a.getScore(),
                        answerRepository.findByAttempt_IdOrderByIdAsc(a.getId()).size(),
                        a.getUser().getEmail().equals(requestingUserEmail)))
                .sorted((a, b) -> b.getScore() - a.getScore())
                .collect(Collectors.toList());

        double averageScore = gradedAttempts.isEmpty() ? 0
                : gradedAttempts.stream().mapToInt(DailyQuizAttempt::getScore).average().orElse(0);
        int maxScore = gradedAttempts.isEmpty() ? QUESTIONS_PER_DAY
                : answerRepository.findByAttempt_IdOrderByIdAsc(gradedAttempts.get(0).getId()).size();

        DailyQuizScoreboardDto dto = new DailyQuizScoreboardDto(entries, averageScore, maxScore);
        gradedAttempts.stream()
                .filter(a -> a.getUser().getEmail().equals(requestingUserEmail))
                .findFirst()
                .ifPresent(a -> dto.setYourLeaderboardPreference(a.isIncludeOnLeaderboard()));
        return dto;
    }

    private DailyQuizAttempt findOrCreateAttempt(DailyQuizSet set, String userEmail) {
        return attemptRepository.findBySet_IdAndUser_Email(set.getId(), userEmail)
                .orElseGet(() -> {
                    AppUser owner = appUserRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new ResourceNotFoundException("No account found for " + userEmail));
                    DailyQuizAttempt fresh = new DailyQuizAttempt();
                    fresh.setSet(set);
                    fresh.setUser(owner);
                    return attemptRepository.save(fresh);
                });
    }
}
