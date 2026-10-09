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
import java.util.LinkedHashMap;
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
// this class mirrors. A set that has reached MAX_AGE_DAYS is deleted by
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
    // 0-based, so index 9 is the 10th question.
    private static final int YEAR_QUESTION_INDEX = 9;
    // A quiz is gone once it is this many days old: a quiz from 7 days ago (or older) is deleted, so
    // today and the 6 days before it are what's left - 7 quizzes in all. Its heavy data (every
    // player's per-question answers) is deleted with it; only the compact per-player result
    // records survive (see DailyQuizResult), which is what the weekly winner is worked out from.
    private static final int MAX_AGE_DAYS = 7;

    private static final Logger log = LoggerFactory.getLogger(DailyQuizService.class);

    private static final int MAX_ANSWER_LENGTH = 1000; // matches the answer_text column
    private static final com.fasterxml.jackson.databind.ObjectMapper DRAFT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    private final DailyQuizSetRepository setRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final DailyQuizAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AppUserRepository appUserRepository;
    private final AdminNotificationService adminNotificationService;
    private final DailyQuizResultService resultService;
    private final ExpertConfig expertConfig;

    public DailyQuizService(DailyQuizSetRepository setRepository,
                             DailyQuizAttemptRepository attemptRepository,
                             DailyQuizAnswerRepository answerRepository,
                             QuestionRepository questionRepository,
                             AppUserRepository appUserRepository,
                             AdminNotificationService adminNotificationService,
                             DailyQuizResultService resultService,
                             ExpertConfig expertConfig) {
        this.setRepository = setRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.appUserRepository = appUserRepository;
        this.adminNotificationService = adminNotificationService;
        this.resultService = resultService;
        this.expertConfig = expertConfig;
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

        // Excludes questions from every set still in storage (ages 0-6, since a quiz that has reached
        // MAX_AGE_DAYS is deleted) - so a question doesn't come round again within the week.
        Set<Long> recentlyUsed = setRepository.findByQuizDateAfter(quizDate.minusDays(MAX_AGE_DAYS))
                .stream()
                .flatMap(s -> s.getQuestionIds().stream())
                .collect(Collectors.toSet());
        // The quiz has two fixed "feature" slots, each filled by exactly ONE question from its own
        // category - never more, never fewer - with every other question drawn from outside both:
        //   position 1  : a Logo question (with a photo - the text alone is just "which airline?")
        //   position 10 : a Year question (several events from one year; answered with that year)
        Question logoQuestion = pickFeatureQuestion(candidates, recentlyUsed, DailyQuizService::isLogoQuestionWithPhoto);
        Question yearQuestion = pickFeatureQuestion(candidates, recentlyUsed, DailyQuizService::isYearQuestionWithYearAnswer);

        List<Question> others = candidates.stream()
                .filter(q -> !isLogoCategory(q) && !DailyQuizScoring.isYearQuestion(q))
                .filter(q -> !recentlyUsed.contains(q.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(others);

        int featureCount = (logoQuestion != null ? 1 : 0) + (yearQuestion != null ? 1 : 0);
        List<Long> questionIds = new ArrayList<>();
        if (logoQuestion != null) {
            questionIds.add(logoQuestion.getId());
        }
        others.stream()
                .limit(QUESTIONS_PER_DAY - featureCount)
                .map(Question::getId)
                .forEach(questionIds::add);
        if (yearQuestion != null) {
            // Slot 10 (index 9) - or the end, if there somehow aren't enough questions to reach it.
            questionIds.add(Math.min(YEAR_QUESTION_INDEX, questionIds.size()), yearQuestion.getId());
        }

        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(quizDate);
        set.setQuestionIds(questionIds);
        return setRepository.save(set);
    }

    // A random question matching `eligible`, preferring ones not used within the retention window.
    // If every eligible one HAS been used recently, repeating one beats a quiz with that slot
    // missing altogether. Null only if nothing eligible exists at all.
    private static Question pickFeatureQuestion(List<Question> candidates, Set<Long> recentlyUsed,
                                                java.util.function.Predicate<Question> eligible) {
        List<Question> pool = candidates.stream().filter(eligible).collect(Collectors.toList());
        List<Question> fresh = pool.stream().filter(q -> !recentlyUsed.contains(q.getId())).collect(Collectors.toList());
        List<Question> choices = fresh.isEmpty() ? pool : fresh;
        return choices.isEmpty() ? null : choices.get(new Random().nextInt(choices.size()));
    }

    // A Year question needs a stored answer that actually contains a year - otherwise it couldn't be
    // scored, so it's not eligible to be the daily one (it's still kept out of the ordinary slots).
    private static boolean isYearQuestionWithYearAnswer(Question q) {
        return DailyQuizScoring.isYearQuestion(q) && DailyQuizScoring.parseYear(q.getAnswer()) != null;
    }

    private static boolean isLogoCategory(Question q) {
        return q.getCategory() != null && LOGO_CATEGORY.equalsIgnoreCase(q.getCategory().trim());
    }

    private static boolean isLogoQuestionWithPhoto(Question q) {
        return isLogoCategory(q) && q.getPhotoUrl() != null && !q.getPhotoUrl().isBlank();
    }

    /** The newest quiz date that's already too old to keep: a quiz from this day (or earlier) is expired. */
    static LocalDate expiryCutoff() {
        return LocalDate.now().minusDays(MAX_AGE_DAYS);
    }

    /** True once a quiz is MAX_AGE_DAYS old - it's hidden from everyone from that moment, even before the cleanup job below gets round to deleting it. */
    public static boolean isExpired(LocalDate quizDate) {
        return !quizDate.isAfter(expiryCutoff());
    }

    // Runs hourly, same cadence as RoomCleanupService - deletes every quiz that has reached
    // MAX_AGE_DAYS (with its attempts and answers), and prunes the compact result records that are
    // much older still. The result records are NOT deleted with the quiz: they're what the weekly
    // winner is calculated from, and a week's first day is exactly this old when the week ends.
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void deleteOldSets() {
        resultService.pruneOld(LocalDate.now());

        List<DailyQuizSet> stale = setRepository.findByQuizDateLessThanEqual(expiryCutoff());
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
        log.info("Daily quiz cleanup: removed {} quiz(zes) that reached {} days old", stale.size(), MAX_AGE_DAYS);
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
        // Bounded below as well as above: a quiz that's reached MAX_AGE_DAYS must disappear at once,
        // not whenever the hourly cleanup next runs.
        List<DailyQuizSet> pastSets = setRepository.findByQuizDateAfterAndQuizDateBeforeOrderByQuizDateDesc(
                expiryCutoff(), LocalDate.now());
        return toSummaries(pastSets, userEmail);
    }

    private List<DailyQuizSetSummaryDto> toSummaries(List<DailyQuizSet> sets, String userEmail) {
        List<Long> setIds = sets.stream().map(DailyQuizSet::getId).collect(Collectors.toList());
        Map<Long, DailyQuizAttempt> attemptBySetId = setIds.isEmpty()
                ? Map.of()
                : attemptRepository.findBySet_IdInAndUser_Email(setIds, userEmail).stream()
                        .collect(Collectors.toMap(a -> a.getSet().getId(), a -> a));

        // One lookup for every question across all these sets, to work out each set's max score
        // (a Year question is worth 2, so "number of questions" is no longer the max).
        List<Long> allQuestionIds = sets.stream().flatMap(set -> set.getQuestionIds().stream()).distinct().collect(Collectors.toList());
        Map<Long, Question> questionsById = allQuestionIds.isEmpty()
                ? Map.of()
                : questionRepository.findAllById(allQuestionIds).stream().collect(Collectors.toMap(Question::getId, q -> q));

        return sets.stream().map(set -> {
            DailyQuizAttempt attempt = attemptBySetId.get(set.getId());
            String status = attempt == null ? "NOT_STARTED" : attempt.getStatus().name();
            Integer score = attempt != null && attempt.getStatus() == DailyQuizAttemptStatus.GRADED ? attempt.getScore() : null;
            int maxScore = set.getQuestionIds().stream()
                    .map(questionsById::get)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(DailyQuizScoring::maxPoints)
                    .sum();
            return new DailyQuizSetSummaryDto(set.getId(), set.getQuizDate(), set.getQuestionIds().size(), maxScore, status, score);
        }).collect(Collectors.toList());
    }

    @Transactional
    public DailyQuizPlayStateDto getPlayState(Long setId, String userEmail) {
        DailyQuizSet set = requireSet(setId);
        DailyQuizAttempt attempt = findOrCreateAttempt(set, userEmail);
        // Opening a quiz whose result is ready is seeing it - no need to also pop up about it.
        if (attempt.isReviewResultUnseen() && attempt.getStatus() == DailyQuizAttemptStatus.GRADED) {
            attempt.setReviewResultUnseen(false);
            attemptRepository.save(attempt);
        }
        return toPlayStateDto(set, attempt);
    }

    // Results an admin has finished reviewing that this player hasn't looked at yet (newest first).
    // Quizzes that have since expired are left out - there's nothing to open any more.
    @Transactional(readOnly = true)
    public List<com.quizapp.dto.DailyQuizReviewNotificationDto> getReviewNotifications(String userEmail) {
        return attemptRepository.findByUser_EmailAndReviewResultUnseenTrue(userEmail).stream()
                .filter(a -> a.getStatus() == DailyQuizAttemptStatus.GRADED)
                .filter(a -> !isExpired(a.getSet().getQuizDate()))
                .sorted((a, b) -> b.getSet().getQuizDate().compareTo(a.getSet().getQuizDate()))
                .map(a -> {
                    int maxScore = answerRepository.findByAttempt_IdOrderByIdAsc(a.getId()).stream()
                            .mapToInt(ans -> DailyQuizScoring.maxPoints(ans.getQuestion())).sum();
                    return new com.quizapp.dto.DailyQuizReviewNotificationDto(a.getSet().getId(), a.getSet().getQuizDate(), a.getScore(), maxScore);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void dismissReviewNotification(Long setId, String userEmail) {
        attemptRepository.findBySet_IdAndUser_Email(setId, userEmail).ifPresent(a -> {
            if (a.isReviewResultUnseen()) {
                a.setReviewResultUnseen(false);
                attemptRepository.save(a);
            }
        });
    }

    private DailyQuizSet requireSet(Long setId) {
        return setRepository.findById(setId)
                .filter(set -> !isExpired(set.getQuizDate())) // expired = gone, even if the cleanup job hasn't deleted it yet
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
                    questionDtos.add(new DailyQuizPlayStateDto.QuestionDto(i + 1, q.getId(), q.getQuestionText(), q.getPhotoUrl(), DailyQuizScoring.isScoredAsYear(q)));
                }
            }
            dto.setQuestions(questionDtos);
            dto.setDraftAnswers(readDraft(attempt));
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
        // Not simply "number of answers": the Year question is worth up to 2.
        result.setMaxScore(answers.stream().mapToInt(a -> DailyQuizScoring.maxPoints(a.getQuestion())).sum());
        List<DailyQuizResultDto.AnswerResultDto> rows = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            DailyQuizAnswer a = answers.get(i);
            boolean pending = a.getVerdict() == DailyQuizAnswerVerdict.PENDING;
            Integer pointsAwarded = pending ? null : (a.getVerdict() == DailyQuizAnswerVerdict.CORRECT ? a.getPoints() : 0);
            rows.add(new DailyQuizResultDto.AnswerResultDto(
                    i + 1, a.getQuestion().getQuestionText(), a.getAnswerText(),
                    pending ? null : a.getQuestion().getAnswer(), a.getVerdict().name(), a.getQuestion().getPhotoUrl(),
                    DailyQuizScoring.isScoredAsYear(a.getQuestion()), pointsAwarded));
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
                answer.setPoints(0);
            } else if (DailyQuizScoring.isScoredAsYear(question)) {
                // Graded by how close the year is (2 / 1 / 0) - purely arithmetic, so it never
                // goes to an admin and never sits in PENDING.
                int points = DailyQuizScoring.yearPoints(trimmed, question.getAnswer());
                answer.setVerdict(points > 0 ? DailyQuizAnswerVerdict.CORRECT : DailyQuizAnswerVerdict.INCORRECT);
                answer.setPoints(points);
            } else if (trimmed.equalsIgnoreCase(question.getAnswer().trim())) {
                answer.setVerdict(DailyQuizAnswerVerdict.CORRECT);
            } else {
                answer.setVerdict(DailyQuizAnswerVerdict.PENDING);
                pendingCount++;
            }
            answerRepository.save(answer);
        }

        attempt.setSubmittedAt(java.time.Instant.now());
        attempt.setDraftAnswers(null);
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

    // "Save for later": keeps what the player has typed so far, server-side so it follows them to
    // another device. Nothing is graded and nothing reaches the admin queue - that only happens on
    // submit. Only answers to this quiz's own questions are kept, and blanks are dropped.
    @Transactional
    public void saveDraft(Long setId, String userEmail, DailyQuizSubmitRequest request) {
        DailyQuizSet set = requireSet(setId);
        DailyQuizAttempt attempt = findOrCreateAttempt(set, userEmail);
        if (attempt.getStatus() != DailyQuizAttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("You've already submitted this quiz.");
        }
        Set<Long> allowed = Set.copyOf(set.getQuestionIds());
        Map<Long, String> draft = new LinkedHashMap<>();
        if (request.getAnswers() != null) {
            for (DailyQuizSubmitRequest.AnswerSubmission a : request.getAnswers()) {
                if (a.getQuestionId() == null || !allowed.contains(a.getQuestionId()) || a.getAnswerText() == null) continue;
                String text = a.getAnswerText().strip();
                if (text.isEmpty()) continue;
                draft.put(a.getQuestionId(), text.length() > MAX_ANSWER_LENGTH ? text.substring(0, MAX_ANSWER_LENGTH) : text);
            }
        }
        try {
            attempt.setDraftAnswers(draft.isEmpty() ? null : DRAFT_MAPPER.writeValueAsString(draft));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not save your answers.", e);
        }
        attemptRepository.save(attempt);
    }

    private Map<Long, String> readDraft(DailyQuizAttempt attempt) {
        if (attempt.getDraftAnswers() == null || attempt.getDraftAnswers().isBlank()) return Map.of();
        try {
            return DRAFT_MAPPER.readValue(attempt.getDraftAnswers(), new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<Long, String>>() {});
        } catch (java.io.IOException e) {
            log.warn("Unreadable daily quiz draft on attempt {} - ignoring it", attempt.getId(), e);
            return Map.of();
        }
    }

    // Called both right after submit (when nothing needs review) and by
    // DailyQuizReviewService.resolve (once the last pending answer for this
    // attempt is resolved).
    void gradeAttempt(DailyQuizAttempt attempt) {
        // Sum of points, not a head-count: a CORRECT Year answer can be worth 2.
        long score = answerRepository.sumPointsByAttemptAndVerdict(attempt.getId(), DailyQuizAnswerVerdict.CORRECT);
        attempt.setScore((int) score);
        attempt.setStatus(DailyQuizAttemptStatus.GRADED);
        // Also keep the compact record the weekly standings use - it outlives this quiz.
        resultService.record(attempt);
    }

    @Transactional
    public void setLeaderboardPreference(Long setId, String userEmail, boolean includeOnLeaderboard) {
        DailyQuizAttempt attempt = attemptRepository.findBySet_IdAndUser_Email(setId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found for this daily quiz."));
        attempt.setIncludeOnLeaderboard(includeOnLeaderboard);
        attemptRepository.save(attempt);
        resultService.setIncludeOnLeaderboard(attempt, includeOnLeaderboard);
    }

    @Transactional(readOnly = true)
    public DailyQuizScoreboardDto getScoreboard(Long setId, String requestingUserEmail) {
        requireSet(setId); // 404s if the set doesn't exist at all

        List<DailyQuizAttempt> allGraded = attemptRepository.findBySet_Id(setId).stream()
                .filter(a -> a.getStatus() == DailyQuizAttemptStatus.GRADED)
                .collect(Collectors.toList());
        // The expert is the benchmark, not a competitor: kept out of the ranking and the average, shown on their own.
        DailyQuizAttempt expertAttempt = allGraded.stream()
                .filter(a -> expertConfig.isExpert(a.getUser().getId())).findFirst().orElse(null);
        List<DailyQuizAttempt> gradedAttempts = allGraded.stream()
                .filter(a -> !expertConfig.isExpert(a.getUser().getId()))
                .collect(Collectors.toList());

        // Each attempt's max points (a Year question is worth 2, so it isn't just "number of answers"),
        // from one query for all of them rather than one per leaderboard row.
        List<Long> attemptIds = allGraded.stream().map(DailyQuizAttempt::getId).collect(Collectors.toList());
        Map<Long, Integer> maxByAttemptId = attemptIds.isEmpty()
                ? Map.of()
                : answerRepository.findByAttempt_IdIn(attemptIds).stream()
                        .collect(Collectors.groupingBy(a -> a.getAttempt().getId(),
                                Collectors.summingInt(a -> DailyQuizScoring.maxPoints(a.getQuestion()))));

        List<DailyQuizScoreboardEntryDto> entries = gradedAttempts.stream()
                // Opted-out players still count toward the average (just a number,
                // doesn't reveal who they are) but are excluded from the visible
                // ranked list - except your own row, which you can always see.
                .filter(a -> a.isIncludeOnLeaderboard() || a.getUser().getEmail().equals(requestingUserEmail))
                .map(a -> new DailyQuizScoreboardEntryDto(
                        a.getUser().getName(), a.getScore(),
                        maxByAttemptId.getOrDefault(a.getId(), QUESTIONS_PER_DAY),
                        a.getUser().getEmail().equals(requestingUserEmail)))
                .sorted((a, b) -> b.getScore() - a.getScore())
                .collect(Collectors.toList());

        double averageScore = gradedAttempts.isEmpty() ? 0
                : gradedAttempts.stream().mapToInt(DailyQuizAttempt::getScore).average().orElse(0);
        int maxScore = allGraded.isEmpty() ? QUESTIONS_PER_DAY
                : maxByAttemptId.getOrDefault(allGraded.get(0).getId(), QUESTIONS_PER_DAY);

        DailyQuizScoreboardDto dto = new DailyQuizScoreboardDto(entries, averageScore, maxScore);
        if (expertAttempt != null) {
            dto.setExpert(new DailyQuizScoreboardDto.ExpertScore(expertAttempt.getUser().getName(), expertAttempt.getScore(),
                    maxByAttemptId.getOrDefault(expertAttempt.getId(), QUESTIONS_PER_DAY)));
        }
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
