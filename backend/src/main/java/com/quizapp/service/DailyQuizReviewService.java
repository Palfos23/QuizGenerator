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
import com.quizapp.model.DailyQuizAnswerRule;
import com.quizapp.model.DailyQuizComplaint;
import com.quizapp.model.DailyQuizDecisionSource;
import com.quizapp.model.DailyQuizSet;
import com.quizapp.dto.DailyQuizReviewDto;
import com.quizapp.dto.DailyQuizReviewRow;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAnswerRuleRepository;
import com.quizapp.repository.DailyQuizComplaintRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
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
    private final DailyQuizAnswerRuleRepository ruleRepository;
    private final DailyQuizComplaintRepository complaintRepository;

    public DailyQuizReviewService(DailyQuizAnswerRepository answerRepository,
                                   DailyQuizAttemptRepository attemptRepository,
                                   DailyQuizSetRepository setRepository,
                                   DailyQuizService dailyQuizService,
                                   QuestionRepository questionRepository,
                                   DailyQuizAnswerRuleRepository ruleRepository,
                                   DailyQuizComplaintRepository complaintRepository) {
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.setRepository = setRepository;
        this.dailyQuizService = dailyQuizService;
        this.questionRepository = questionRepository;
        this.ruleRepository = ruleRepository;
        this.complaintRepository = complaintRepository;
    }

    // How each of the day's questions went (see DailyQuizService#questionStats) - shown next to the question
    // in the admin's question list, where a very low figure is often a question or answer that needs fixing.
    @Transactional(readOnly = true)
    public List<com.quizapp.dto.DailyQuizQuestionStatDto> listQuestionStatsForSet(Long setId) {
        setRepository.findById(setId)
                .filter(found -> !DailyQuizService.isExpired(found.getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke daglig quiz med id " + setId));
        return new java.util.ArrayList<>(dailyQuizService.questionStats(setId).values());
    }

    // The day's questions in the order players see them, with their stored answers - so an admin can
    // spot and fix a wrong question or answer without hunting for it in the question bank. A question
    // that has since been deleted from the bank is simply left out.
    @Transactional(readOnly = true)
    public List<QuestionDto> listQuestionsForSet(Long setId) {
        DailyQuizSet set = setRepository.findById(setId)
                .filter(found -> !DailyQuizService.isExpired(found.getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke daglig quiz med id " + setId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke forsøk med id " + attemptId));
        List<DailyQuizAnswer> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attemptId);

        Map<Long, com.quizapp.dto.DailyQuizQuestionStatDto> stats = dailyQuizService.questionStats(attempt.getSet().getId());
        List<DailyQuizAttemptDetailDto.AnswerDto> rows = new java.util.ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            DailyQuizAnswer a = answers.get(i);
            DailyQuizAttemptDetailDto.AnswerDto row = new DailyQuizAttemptDetailDto.AnswerDto(
                    a.getId(), i + 1, a.getQuestion().getQuestionText(), a.getAnswerText(),
                    a.getQuestion().getAnswer(), a.getVerdict().name(), a.getQuestion().getPhotoUrl(),
                    DailyQuizScoring.isScoredAsYear(a.getQuestion()), a.getPoints(), isReviewable(a));
            var stat = stats.get(a.getQuestion().getId());
            if (stat != null) {
                row.setPercentCorrect(stat.getPercentCorrect());
                row.setPlayersCounted(stat.getPlayers());
            }
            rows.add(row);
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
        return listAttemptsForSet(setId, "", "ALL", 0, MAX_DAY_PAGE_SIZE);
    }

    public static final int MAX_DAY_PAGE_SIZE = 200;

    /**
     * One page of a day's players, sorted by name. With a lot of players, only the page's rows have their answers
     * loaded (for the score and "still waiting" count); the search (part of the name, any case) and the filter
     * (ALL / PENDING = still waiting on a decision / GRADED) run over the attempts alone.
     */
    @Transactional(readOnly = true)
    public DailyQuizDayAttemptsDto listAttemptsForSet(Long setId, String query, String filter, int page, int size) {
        DailyQuizSet set = setRepository.findById(setId)
                .filter(found -> !DailyQuizService.isExpired(found.getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke daglig quiz med id " + setId));
        List<DailyQuizAttempt> submitted = attemptRepository.findBySet_Id(setId).stream()
                .filter(a -> a.getStatus() != DailyQuizAttemptStatus.IN_PROGRESS)
                .sorted(Comparator.comparing((DailyQuizAttempt a) -> a.getUser().getName(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(DailyQuizAttempt::getId))
                .collect(Collectors.toList());
        int gradedTotal = (int) submitted.stream().filter(a -> a.getStatus() == DailyQuizAttemptStatus.GRADED).count();

        String needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        String wanted = filter == null ? "ALL" : filter.trim().toUpperCase(java.util.Locale.ROOT);
        List<DailyQuizAttempt> matching = submitted.stream()
                .filter(a -> needle.isEmpty() || (a.getUser().getName() != null
                        && a.getUser().getName().toLowerCase(java.util.Locale.ROOT).contains(needle)))
                .filter(a -> switch (wanted) {
                    case "PENDING" -> a.getStatus() != DailyQuizAttemptStatus.GRADED;
                    case "GRADED" -> a.getStatus() == DailyQuizAttemptStatus.GRADED;
                    default -> true;
                })
                .collect(Collectors.toList());

        int pageSize = Math.max(1, Math.min(size, MAX_DAY_PAGE_SIZE));
        int lastPage = Math.max(0, (matching.size() - 1) / pageSize);
        int pageIndex = Math.max(0, Math.min(page, lastPage));
        List<DailyQuizAttempt> attempts = matching.subList(
                Math.min(pageIndex * pageSize, matching.size()), Math.min((pageIndex + 1) * pageSize, matching.size()));

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
        }).collect(Collectors.toList()); // already in name order
        return new DailyQuizDayAttemptsDto(set.getQuizDate(), rows, pageIndex, pageSize,
                matching.size(), submitted.size(), submitted.size() - gradedTotal, gradedTotal);
    }

    /**
     * Whether an admin can (re)decide this answer. Not the ones that are never a judgement call:
     * a Year question (graded by arithmetic), a blank answer (nothing to judge), or an exact match to
     * the stored answer (can't be wrong). Everything else - including answers an admin already
     * decided - can be changed, so a mis-click is fixable.
     */
    static boolean isReviewable(DailyQuizAnswer answer) {
        return isReviewable(answer.getAnswerText(), answer.getQuestion());
    }

    static boolean isReviewable(String given, Question question) {
        if (given == null || given.isBlank()) return false;
        if (DailyQuizScoring.isScoredAsYear(question)) return false;
        return !given.trim().equalsIgnoreCase(question.getAnswer().trim());
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
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke svar med id " + answerId));
        if (!isReviewable(answer)) {
            throw new IllegalStateException("Dette svaret rettes automatisk og kan ikke endres.");
        }
        if (setVerdict(answer, correct, DailyQuizDecisionSource.ADMIN)) {
            afterDecision(answer.getAttempt());
        }
    }

    /**
     * Overrides the automatic grading of a Year answer: the arithmetic only reads the digits that
     * were written, so "88" for 1988 scores 0 although the player clearly meant it. The admin sets
     * the points directly (0, 1 or 2); a graded attempt is re-scored straight away, and the weekly
     * results and leagues follow the same way they do for any other changed decision.
     */
    @Transactional
    public void setYearPoints(Long answerId, int points) {
        if (points < 0 || points > DailyQuizScoring.EXACT_YEAR_POINTS) {
            throw new IllegalArgumentException("Poeng må være mellom 0 og " + DailyQuizScoring.EXACT_YEAR_POINTS + ".");
        }
        DailyQuizAnswer answer = answerRepository.findById(answerId)
                .filter(a -> !DailyQuizService.isExpired(a.getAttempt().getSet().getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke svar med id " + answerId));
        if (!DailyQuizScoring.isScoredAsYear(answer.getQuestion())) {
            throw new IllegalStateException("Dette er ikke et årstallsspørsmål.");
        }
        DailyQuizAnswerVerdict verdict = points > 0 ? DailyQuizAnswerVerdict.CORRECT : DailyQuizAnswerVerdict.INCORRECT;
        int current = answer.getVerdict() == DailyQuizAnswerVerdict.CORRECT ? answer.getPoints() : 0;
        if (current == points && answer.getVerdict() == verdict) return;
        answer.setVerdict(verdict);
        answer.setPoints(points > 0 ? points : 0);
        answer.setDecidedBy(DailyQuizDecisionSource.ADMIN);
        answerRepository.save(answer);
        afterDecision(answer.getAttempt());
    }

    /**
     * Records a verdict on one answer. Returns false when nothing changed (already decided that way - a double click, or
     * a page that was a little stale). A complaint still waiting on an answer that is now correct is settled in the
     * player's favour, since that is exactly what they asked for.
     */
    boolean setVerdict(DailyQuizAnswer answer, boolean correct, DailyQuizDecisionSource source) {
        DailyQuizAnswerVerdict decision = correct ? DailyQuizAnswerVerdict.CORRECT : DailyQuizAnswerVerdict.INCORRECT;
        if (answer.getVerdict() == decision) return false;
        answer.setVerdict(decision);
        answer.setPoints(1); // an ordinary question is worth 1 when correct
        answer.setDecidedBy(source);
        answerRepository.save(answer);
        if (correct) {
            complaintRepository.findByAnswer_Id(answer.getId())
                    .filter(c -> c.getStatus() == DailyQuizComplaint.Status.OPEN)
                    .ifPresent(c -> {
                        c.setStatus(DailyQuizComplaint.Status.ACCEPTED);
                        c.setResolvedAt(Instant.now());
                        complaintRepository.save(c);
                    });
        }
        return true;
    }

    /**
     * Once nothing is left waiting on an attempt, grade it - for the first time, or again when a decision on an
     * already-graded one changed. Worth telling the player about: their score just became final, or it moved.
     */
    void afterDecision(DailyQuizAttempt attempt) {
        long stillPending = answerRepository.countByAttempt_IdAndVerdict(attempt.getId(), DailyQuizAnswerVerdict.PENDING);
        if (stillPending != 0) return;
        boolean firstGrading = attempt.getStatus() != DailyQuizAttemptStatus.GRADED;
        int scoreBefore = attempt.getScore();
        dailyQuizService.gradeAttempt(attempt);
        if (firstGrading || attempt.getScore() != scoreBefore) {
            attempt.setReviewResultUnseen(true);
        }
        attemptRepository.save(attempt);
    }

    // ------------------------------------------------------------------ per-answer review

    /**
     * Every answer the admin may judge, across the quizzes still around, grouped by day, question and - the point of it -
     * by what was written: the same answer from 30 players is one line, decided once. Pending groups come first, the ones
     * that look like a typo of the right answer leading them.
     */
    @Transactional(readOnly = true)
    public DailyQuizReviewDto.AnswerGroups listAnswerGroups() {
        return buildGroups();
    }

    private DailyQuizReviewDto.AnswerGroups buildGroups() {
        // Few queries for the whole page, however many answers there are: the quizzes, then every answer as a plain
        // row, then the questions and remembered rules - not one query per quiz, question or player.
        List<DailyQuizSet> sets = setRepository.findByQuizDateAfter(DailyQuizService.expiryCutoff()).stream()
                .sorted(Comparator.comparing(DailyQuizSet::getQuizDate).reversed())
                .collect(Collectors.toList());
        if (sets.isEmpty()) return new DailyQuizReviewDto.AnswerGroups(new ArrayList<>(), 0, 0);
        List<DailyQuizReviewRow> allRows = answerRepository.findReviewRows(
                sets.stream().map(DailyQuizSet::getId).collect(Collectors.toList()), DailyQuizAttemptStatus.IN_PROGRESS);
        Map<Long, Question> questionsById = questionRepository.findAllById(
                allRows.stream().map(DailyQuizReviewRow::questionId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        // Only answers that need a decision (a blank one, an exact match, or a Year question grades itself).
        Map<Long, Map<Long, List<DailyQuizReviewRow>>> bySetAndQuestion = allRows.stream()
                .filter(r -> questionsById.containsKey(r.questionId()) && isReviewable(r.answerText(), questionsById.get(r.questionId())))
                .collect(Collectors.groupingBy(DailyQuizReviewRow::setId, Collectors.groupingBy(DailyQuizReviewRow::questionId)));
        Map<Long, Map<String, Boolean>> rules = new HashMap<>();
        if (!bySetAndQuestion.isEmpty()) {
            ruleRepository.findByQuestionIdIn(questionsById.keySet()).forEach(r ->
                    rules.computeIfAbsent(r.getQuestionId(), k -> new HashMap<>()).put(r.getNormalizedText(), r.isCorrect()));
        }

        List<DailyQuizReviewDto.Day> days = new ArrayList<>();
        int pendingTotal = 0;
        int typoTotal = 0;
        for (DailyQuizSet set : sets) {
            Map<Long, List<DailyQuizReviewRow>> byQuestion = bySetAndQuestion.get(set.getId());
            if (byQuestion == null || byQuestion.isEmpty()) continue;
            List<DailyQuizReviewDto.QuestionGroups> questions = new ArrayList<>();
            int dayPending = 0;
            for (int i = 0; i < set.getQuestionIds().size(); i++) {
                Long questionId = set.getQuestionIds().get(i);
                List<DailyQuizReviewRow> answers = byQuestion.get(questionId);
                if (answers == null) continue;
                Question question = questionsById.get(questionId);
                Map<String, List<DailyQuizReviewRow>> grouped = answers.stream()
                        .collect(Collectors.groupingBy(a -> AnswerText.key(a.answerText()), LinkedHashMap::new, Collectors.toList()));
                List<DailyQuizReviewDto.Group> groups = new ArrayList<>();
                for (Map.Entry<String, List<DailyQuizReviewRow>> g : grouped.entrySet()) {
                    List<DailyQuizReviewRow> list = g.getValue();
                    int pending = (int) list.stream().filter(a -> a.verdict() == DailyQuizAnswerVerdict.PENDING).count();
                    int correct = (int) list.stream().filter(a -> a.verdict() == DailyQuizAnswerVerdict.CORRECT).count();
                    groups.add(new DailyQuizReviewDto.Group(mostCommonSpelling(list), g.getKey(), list.size(), pending, correct,
                            list.size() - pending - correct, AnswerText.likelyTypo(list.get(0).answerText(), question.getAnswer()),
                            rules.getOrDefault(questionId, Map.of()).get(g.getKey())));
                }
                groups.sort(Comparator.comparing((DailyQuizReviewDto.Group x) -> x.pendingCount() == 0)
                        .thenComparing(x -> !x.likelyTypo())
                        .thenComparing(Comparator.comparingInt(DailyQuizReviewDto.Group::count).reversed())
                        .thenComparing(DailyQuizReviewDto.Group::key));
                dayPending += groups.stream().mapToInt(DailyQuizReviewDto.Group::pendingCount).sum();
                typoTotal += groups.stream().filter(x -> x.pendingCount() > 0 && x.likelyTypo()).mapToInt(DailyQuizReviewDto.Group::pendingCount).sum();
                questions.add(new DailyQuizReviewDto.QuestionGroups(questionId, i + 1, question.getQuestionText(), question.getAnswer(), groups));
            }
            pendingTotal += dayPending;
            days.add(new DailyQuizReviewDto.Day(set.getId(), set.getQuizDate(), dayPending, questions));
        }
        return new DailyQuizReviewDto.AnswerGroups(days, pendingTotal, typoTotal);
    }

    private static String mostCommonSpelling(List<DailyQuizReviewRow> list) {
        return list.stream().collect(Collectors.groupingBy(a -> a.answerText().trim(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .orElseThrow().getKey();
    }

    /**
     * Decides one answer to one question for everybody who wrote it (anywhere the quiz still exists), and - when asked to
     * remember - for everybody who writes it later too. Returns how many answers changed.
     */
    @Transactional
    public int decideGroup(Long questionId, String text, boolean correct, boolean remember) {
        String key = AnswerText.key(text);
        if (key.isEmpty()) throw new IllegalArgumentException("Mangler svaret.");
        if (remember) {
            DailyQuizAnswerRule rule = ruleRepository.findByQuestionIdAndNormalizedText(questionId, key).orElseGet(DailyQuizAnswerRule::new);
            rule.setQuestionId(questionId);
            rule.setNormalizedText(key);
            rule.setCorrect(correct);
            ruleRepository.save(rule);
        }
        Set<DailyQuizAttempt> affected = new LinkedHashSet<>();
        int changed = 0;
        for (DailyQuizAnswer a : answerRepository.findByQuestion_Id(questionId)) {
            if (DailyQuizService.isExpired(a.getAttempt().getSet().getQuizDate())) continue;
            if (a.getAttempt().getStatus() == DailyQuizAttemptStatus.IN_PROGRESS) continue;
            if (!isReviewable(a) || !AnswerText.key(a.getAnswerText()).equals(key)) continue;
            if (setVerdict(a, correct, remember ? DailyQuizDecisionSource.RULE : DailyQuizDecisionSource.ADMIN)) {
                changed++;
                affected.add(a.getAttempt());
            }
        }
        affected.forEach(this::afterDecision);
        return changed;
    }

    /** Applies a batch of group decisions in one transaction. Returns how many answers changed in all. */
    @Transactional
    public int decideGroups(List<DailyQuizReviewDto.DecideGroupRequest> decisions) {
        if (decisions == null || decisions.size() > 500) throw new IllegalArgumentException("Ugyldig antall avgjørelser.");
        int changed = 0;
        for (DailyQuizReviewDto.DecideGroupRequest d : decisions) {
            changed += decideGroup(d.questionId(), d.text(), d.correct(), d.remember());
        }
        return changed;
    }

    /** Accepts, and remembers, every waiting answer that looks like a misspelling of the right one (optionally one day only). */
    @Transactional
    public int acceptLikelyTypos(Long setId) {
        int changed = 0;
        for (DailyQuizReviewDto.Day day : buildGroups().days()) {
            if (setId != null && !setId.equals(day.setId())) continue;
            for (DailyQuizReviewDto.QuestionGroups q : day.questions()) {
                for (DailyQuizReviewDto.Group g : q.groups()) {
                    if (g.pendingCount() > 0 && g.likelyTypo()) changed += decideGroup(q.questionId(), g.text(), true, true);
                }
            }
        }
        return changed;
    }
}
