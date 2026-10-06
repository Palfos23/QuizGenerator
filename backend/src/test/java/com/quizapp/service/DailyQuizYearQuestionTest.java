package com.quizapp.service;

import com.quizapp.dto.DailyQuizPlayStateDto;
import com.quizapp.dto.DailyQuizSetSummaryDto;
import com.quizapp.dto.DailyQuizSubmitRequest;
import com.quizapp.model.AppUser;
import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizSet;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizSetRepository;
import com.quizapp.repository.QuestionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// The daily Year question: sits at position 10, is the only one of its kind in a quiz, and is graded by
// how close the guess is - 2 points for the exact year, 1 for one year off - automatically (no admin).
//
// Everything this class creates is removed afterwards. The rest of the daily quiz tests share this
// in-memory database and assume today's quiz is made of ordinary questions with 1 point each; a
// Year-category question left lying around could get drawn into that quiz and change the arithmetic.
@SpringBootTest
class DailyQuizYearQuestionTest {

    @Autowired
    private DailyQuizService dailyQuizService;
    @Autowired
    private DailyQuizReviewService dailyQuizReviewService;
    @Autowired
    private DailyQuizSetRepository setRepository;
    @Autowired
    private DailyQuizAttemptRepository attemptRepository;
    @Autowired
    private DailyQuizAnswerRepository answerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    private static final AtomicInteger DAY_COUNTER = new AtomicInteger();

    private final List<Question> createdQuestions = new ArrayList<>();
    private final List<DailyQuizSet> createdSets = new ArrayList<>();

    // Children before parents: answers -> attempts -> sets -> the questions themselves.
    @AfterEach
    void cleanUp() {
        List<Long> setIds = createdSets.stream().map(DailyQuizSet::getId).toList();
        if (!setIds.isEmpty()) {
            List<DailyQuizAttempt> attempts = attemptRepository.findBySet_IdIn(setIds);
            List<Long> attemptIds = attempts.stream().map(DailyQuizAttempt::getId).toList();
            if (!attemptIds.isEmpty()) {
                answerRepository.deleteByAttempt_IdIn(attemptIds);
            }
            attemptRepository.deleteAll(attempts);
            setRepository.deleteAll(createdSets);
        }
        questionRepository.deleteAll(createdQuestions);
        createdSets.clear();
        createdQuestions.clear();
    }

    private Question newQuestion(String category, String text, String answer, String photoUrl) {
        Question q = new Question();
        q.setQuestionText(text);
        q.setCategory(category);
        q.setDifficultyLevel(5);
        q.setLanguage(Language.NO);
        q.setAnswer(answer);
        q.setPhotoUrl(photoUrl);
        q = questionRepository.save(q);
        createdQuestions.add(q);
        return q;
    }

    private DailyQuizSet newSet(LocalDate date, List<Long> questionIds) {
        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(date);
        set.setQuestionIds(questionIds);
        set = setRepository.save(set);
        createdSets.add(set);
        return set;
    }

    private LocalDate uniqueFarFutureDay() {
        return LocalDate.now().plusYears(30).plusDays(DAY_COUNTER.incrementAndGet());
    }

    private AppUser newUser() {
        String suffix = String.valueOf(System.nanoTime());
        AppUser user = new AppUser();
        user.setEmail("year-player-" + suffix + "@example.com");
        user.setName("Year Player " + suffix);
        return appUserRepository.save(user);
    }

    private DailyQuizPlayStateDto submit(DailyQuizSet set, AppUser user, Question first, String firstAnswer, Question second, String secondAnswer) {
        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (Object[] pair : new Object[][]{{first, firstAnswer}, {second, secondAnswer}}) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(((Question) pair[0]).getId());
            a.setAnswerText((String) pair[1]);
            answers.add(a);
        }
        request.setAnswers(answers);
        return dailyQuizService.submitAnswers(set.getId(), user.getEmail(), request);
    }

    // ---- placement ----

    @Test
    void yearQuestionIsTenthAndTheOnlyOneAndLogoStillComesFirst() {
        for (int i = 0; i < 30; i++) newQuestion("General", "Ordinary " + System.nanoTime() + "-" + i, "Answer", null);
        newQuestion("Logo", "Hvilket selskap? " + System.nanoTime(), "Some brand", "https://example.com/logo.png");
        for (int i = 0; i < 5; i++) newQuestion("Year", "Events of a year " + System.nanoTime() + "-" + i, "19" + (80 + i), null);
        // A Year-category question whose answer isn't a year can never be the daily Year question,
        // and (like every Year question) must not turn up in an ordinary slot either.
        for (int i = 0; i < 3; i++) newQuestion("Year", "Broken year question " + System.nanoTime() + "-" + i, "Unknown", null);

        DailyQuizSet generated = dailyQuizService.generateSet(LocalDate.now().plusYears(10));
        createdSets.add(generated);

        List<Question> inOrder = generated.getQuestionIds().stream()
                .map(id -> questionRepository.findById(id).orElseThrow())
                .toList();
        assertThat(inOrder).hasSize(15);
        assertThat(inOrder.get(0).getCategory()).as("question 1").isEqualTo("Logo");
        assertThat(inOrder.get(9).getCategory()).as("question 10").isEqualTo("Year");
        assertThat(DailyQuizScoring.parseYear(inOrder.get(9).getAnswer())).as("the Year question must be scorable").isNotNull();
        assertThat(inOrder.stream().filter(DailyQuizScoring::isYearQuestion).count()).as("exactly one Year question").isEqualTo(1);
        assertThat(inOrder.stream().filter(q -> "Logo".equalsIgnoreCase(q.getCategory())).count()).as("exactly one Logo question").isEqualTo(1);
    }

    @Test
    void aYearQuestionIsReusedRatherThanLeavingTheTenthSlotOutWhenAllWereRecentlyUsed() {
        for (int i = 0; i < 30; i++) newQuestion("General", "Ordinary " + System.nanoTime() + "-" + i, "Answer", null);
        List<Long> yearIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) yearIds.add(newQuestion("Year", "Events " + System.nanoTime() + "-" + i, "197" + i, null).getId());

        LocalDate day = LocalDate.now().plusYears(11);
        newSet(day.minusDays(1), yearIds); // yesterday's quiz "used" every Year question there is

        DailyQuizSet generated = dailyQuizService.generateSet(day);
        createdSets.add(generated);

        Question tenth = questionRepository.findById(generated.getQuestionIds().get(9)).orElseThrow();
        assertThat(tenth.getCategory()).isEqualTo("Year");
    }

    // ---- scoring ----

    @Test
    void exactYearScoresTwoAndTheQuizGradesAtOnceWithoutAnAdmin() {
        Question year = newQuestion("Year", "Events 1", "1994", null);
        Question ordinary = newQuestion("General", "Capital of Norway?", "Oslo", null);
        DailyQuizSet set = newSet(uniqueFarFutureDay(), List.of(year.getId(), ordinary.getId()));
        AppUser user = newUser();

        DailyQuizPlayStateDto result = submit(set, user, year, "1994", ordinary, "Oslo");

        assertThat(result.getAttemptStatus()).as("nothing is left for an admin to decide").isEqualTo("GRADED");
        assertThat(result.getResult().getScore()).isEqualTo(3);
        assertThat(result.getResult().getMaxScore()).as("year question is worth up to 2").isEqualTo(3);
        var yearRow = result.getResult().getAnswers().get(0);
        assertThat(yearRow.isYearQuestion()).isTrue();
        assertThat(yearRow.getVerdict()).isEqualTo("CORRECT");
        assertThat(yearRow.getPointsAwarded()).isEqualTo(2);
        assertThat(result.getResult().getAnswers().get(1).getPointsAwarded()).isEqualTo(1);
        assertThat(dailyQuizReviewService.listPendingAttempts()).noneMatch(p -> p.getPlayerName().equals(user.getName()));
    }

    @Test
    void oneYearOffScoresOnePointInEitherDirection() {
        Question year = newQuestion("Year", "Events 2", "1994", null);
        Question ordinary = newQuestion("General", "Capital of Norway?", "Oslo", null);
        DailyQuizSet set = newSet(uniqueFarFutureDay(), List.of(year.getId(), ordinary.getId()));

        DailyQuizPlayStateDto early = submit(set, newUser(), year, "1993", ordinary, "Oslo");
        DailyQuizPlayStateDto late = submit(set, newUser(), year, "1995", ordinary, "Oslo");

        for (DailyQuizPlayStateDto r : List.of(early, late)) {
            assertThat(r.getResult().getScore()).isEqualTo(2); // 1 for the year + 1 for Oslo
            assertThat(r.getResult().getAnswers().get(0).getVerdict()).isEqualTo("CORRECT");
            assertThat(r.getResult().getAnswers().get(0).getPointsAwarded()).isEqualTo(1);
        }
    }

    @Test
    void twoYearsOffNonNumericAndBlankGuessesScoreNothing() {
        Question year = newQuestion("Year", "Events 3", "1994", null);
        Question ordinary = newQuestion("General", "Capital of Norway?", "Oslo", null);
        DailyQuizSet set = newSet(uniqueFarFutureDay(), List.of(year.getId(), ordinary.getId()));

        for (String guess : List.of("1996", "banan", "")) {
            DailyQuizPlayStateDto r = submit(set, newUser(), year, guess, ordinary, "Oslo");
            assertThat(r.getAttemptStatus()).as("'%s' is settled automatically, not sent to an admin", guess).isEqualTo("GRADED");
            assertThat(r.getResult().getScore()).as("only Oslo scores for '%s'", guess).isEqualTo(1);
            assertThat(r.getResult().getAnswers().get(0).getVerdict()).isEqualTo("INCORRECT");
            assertThat(r.getResult().getAnswers().get(0).getPointsAwarded()).isZero();
        }
    }

    @Test
    void yearPointsAndAnAdminsDecisionOnAnotherAnswerAddUpTogether() {
        Question year = newQuestion("Year", "Events 4", "1994", null);
        Question ordinary = newQuestion("General", "Capital of Norway?", "Oslo", null);
        DailyQuizSet set = newSet(uniqueFarFutureDay(), List.of(year.getId(), ordinary.getId()));
        AppUser user = newUser();

        // Year exact (2, automatic); the other answer isn't an exact match, so only THAT waits on an admin.
        DailyQuizPlayStateDto submitted = submit(set, user, year, "1994", ordinary, "Oslo, i Norge");
        assertThat(submitted.getAttemptStatus()).isEqualTo("SUBMITTED");
        var pending = dailyQuizReviewService.listPendingAttempts().stream()
                .filter(p -> p.getPlayerName().equals(user.getName())).findFirst().orElseThrow();
        assertThat(pending.getPendingCount()).as("the year answer never needs review").isEqualTo(1);

        var detail = dailyQuizReviewService.getAttemptDetail(pending.getAttemptId());
        assertThat(detail.getAnswers().get(0).isYearQuestion()).isTrue();
        dailyQuizReviewService.resolve(detail.getAnswers().get(1).getAnswerId(), true);

        DailyQuizPlayStateDto graded = dailyQuizService.getPlayState(set.getId(), user.getEmail());
        assertThat(graded.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(graded.getResult().getScore()).as("2 for the exact year + 1 for the answer the admin accepted").isEqualTo(3);
        assertThat(graded.getResult().getMaxScore()).isEqualTo(3);
    }

    // ---- max score everywhere it's shown ----

    @Test
    void theListAndTheScoreboardUseTheRealMaxScoreNotTheNumberOfQuestions() {
        Question year = newQuestion("Year", "Events 5", "1994", null);
        Question ordinary = newQuestion("General", "Capital of Norway?", "Oslo", null);
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        DailyQuizSet set = newSet(twoDaysAgo, List.of(year.getId(), ordinary.getId()));
        AppUser winner = newUser();
        AppUser other = newUser();
        submit(set, winner, year, "1994", ordinary, "Oslo");
        submit(set, other, year, "1995", ordinary, "Oslo");

        DailyQuizSetSummaryDto summary = dailyQuizService.findArchive(winner.getEmail()).stream()
                .filter(s -> s.getId().equals(set.getId())).findFirst().orElseThrow();
        assertThat(summary.getQuestionCount()).isEqualTo(2);
        assertThat(summary.getMaxScore()).as("2 questions, but 3 points available").isEqualTo(3);
        assertThat(summary.getScore()).isEqualTo(3);

        var board = dailyQuizService.getScoreboard(set.getId(), winner.getEmail());
        assertThat(board.getMaxScore()).isEqualTo(3);
        assertThat(board.getEntries()).extracting(e -> e.getScore()).containsExactly(3, 2);
        assertThat(board.getEntries()).allMatch(e -> e.getMaxScore() == 3);
    }
}
