package com.quizapp.service;

import com.quizapp.dto.DailyQuizAttemptDetailDto;
import com.quizapp.dto.DailyQuizPlayStateDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// An admin's decision on a free-text answer must be fixable: marking something correct/incorrect by
// mistake used to be permanent (and, once every answer was decided, the attempt vanished from the
// review list so it couldn't even be found again). Re-marking is now allowed and re-scores the
// attempt; every attempt for a day can be listed; and answers that are never a judgement call
// (exact matches, blanks, the Year question) stay locked.
//
// Builds and removes its own quizzes, since the other daily quiz tests share this database.
@SpringBootTest
class DailyQuizReviewCorrectionTest {

    @Autowired
    private DailyQuizService dailyQuizService;
    @Autowired
    private DailyQuizReviewService reviewService;
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

    private Question newQuestion(String category, String text, String answer) {
        Question q = new Question();
        q.setQuestionText(text);
        q.setCategory(category);
        q.setDifficultyLevel(5);
        q.setLanguage(Language.NO);
        q.setAnswer(answer);
        q = questionRepository.save(q);
        createdQuestions.add(q);
        return q;
    }

    private DailyQuizSet newSet(Question... questions) {
        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(LocalDate.now().plusYears(40).plusDays(DAY_COUNTER.incrementAndGet()));
        set.setQuestionIds(java.util.Arrays.stream(questions).map(Question::getId).toList());
        set = setRepository.save(set);
        createdSets.add(set);
        return set;
    }

    private AppUser newUser() {
        String suffix = String.valueOf(System.nanoTime());
        AppUser user = new AppUser();
        user.setEmail("review-player-" + suffix + "@example.com");
        user.setName("Review Player " + suffix);
        return appUserRepository.save(user);
    }

    private DailyQuizPlayStateDto submit(DailyQuizSet set, AppUser user, String... answersInQuestionOrder) {
        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (int i = 0; i < answersInQuestionOrder.length; i++) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(set.getQuestionIds().get(i));
            a.setAnswerText(answersInQuestionOrder[i]);
            answers.add(a);
        }
        request.setAnswers(answers);
        return dailyQuizService.submitAnswers(set.getId(), user.getEmail(), request);
    }

    private DailyQuizAttemptDetailDto detailFor(AppUser user, DailyQuizSet set) {
        Long attemptId = attemptRepository.findBySet_IdAndUser_Email(set.getId(), user.getEmail()).orElseThrow().getId();
        return reviewService.getAttemptDetail(attemptId);
    }

    @Test
    void aWrongDecisionCanBeChangedAndAGradedAttemptIsRescoredEachTime() {
        Question q1 = newQuestion("General", "Capital of Norway?", "Oslo");
        Question q2 = newQuestion("General", "Capital of Sweden?", "Stockholm");
        DailyQuizSet set = newSet(q1, q2);
        AppUser user = newUser();
        submit(set, user, "Oslo, i Norge", "Stockholm city"); // neither is an exact match -> both wait on an admin

        var answers = detailFor(user, set).getAnswers();
        reviewService.resolve(answers.get(0).getAnswerId(), true);
        reviewService.resolve(answers.get(1).getAnswerId(), true);
        assertThat(dailyQuizService.getPlayState(set.getId(), user.getEmail()).getResult().getScore()).isEqualTo(2);

        // The admin realises the second one shouldn't have counted.
        reviewService.resolve(answers.get(1).getAnswerId(), false);

        DailyQuizPlayStateDto afterFix = dailyQuizService.getPlayState(set.getId(), user.getEmail());
        assertThat(afterFix.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(afterFix.getResult().getScore()).as("re-scored straight away").isEqualTo(1);
        assertThat(afterFix.getResult().getAnswers().get(1).getVerdict()).isEqualTo("INCORRECT");
        assertThat(dailyQuizService.getScoreboard(set.getId(), user.getEmail()).getEntries().get(0).getScore())
                .as("and so does the leaderboard").isEqualTo(1);

        // ...and back again - it works in both directions, any number of times.
        reviewService.resolve(answers.get(1).getAnswerId(), true);
        assertThat(dailyQuizService.getPlayState(set.getId(), user.getEmail()).getResult().getScore()).isEqualTo(2);
    }

    @Test
    void changingADecisionWhileOtherAnswersAreStillWaitingDoesNotGradeTheAttempt() {
        Question q1 = newQuestion("General", "Q one?", "Alpha");
        Question q2 = newQuestion("General", "Q two?", "Beta");
        Question q3 = newQuestion("General", "Q three?", "Gamma");
        DailyQuizSet set = newSet(q1, q2, q3);
        AppUser user = newUser();
        submit(set, user, "Alpha-ish", "Beta-ish", "Gamma-ish");

        var answers = detailFor(user, set).getAnswers();
        reviewService.resolve(answers.get(0).getAnswerId(), true);
        reviewService.resolve(answers.get(0).getAnswerId(), false); // changed their mind, two still waiting

        DailyQuizPlayStateDto state = dailyQuizService.getPlayState(set.getId(), user.getEmail());
        assertThat(state.getAttemptStatus()).isEqualTo("SUBMITTED");
        assertThat(state.getResult().getScore()).isNull();
        assertThat(detailFor(user, set).getAnswers().get(0).getVerdict()).isEqualTo("INCORRECT");
    }

    @Test
    void markingAnAnswerTheWayItAlreadyIsIsHarmless() {
        Question q = newQuestion("General", "Capital of Norway?", "Oslo");
        DailyQuizSet set = newSet(q);
        AppUser user = newUser();
        submit(set, user, "Oslo, i Norge");
        Long answerId = detailFor(user, set).getAnswers().get(0).getAnswerId();

        reviewService.resolve(answerId, true);
        reviewService.resolve(answerId, true); // e.g. a double click

        assertThat(dailyQuizService.getPlayState(set.getId(), user.getEmail()).getResult().getScore()).isEqualTo(1);
    }

    @Test
    void answersThatAreNeverAJudgementCallStayLocked() {
        Question exact = newQuestion("General", "Capital of Norway?", "Oslo");
        Question blank = newQuestion("General", "Capital of Sweden?", "Stockholm");
        Question year = newQuestion("Year", "Events of a year", "1994");
        Question judged = newQuestion("General", "Capital of Denmark?", "Copenhagen");
        DailyQuizSet set = newSet(exact, blank, year, judged);
        AppUser user = newUser();
        submit(set, user, "oslo", "", "1995", "Copenhagen, Danmark");

        var answers = detailFor(user, set).getAnswers();
        assertThat(answers).extracting(a -> a.isReviewable()).containsExactly(false, false, false, true);
        for (int i = 0; i < 3; i++) {
            Long id = answers.get(i).getAnswerId();
            assertThatThrownBy(() -> reviewService.resolve(id, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("graded automatically");
        }
        // the one real judgement call can still be made
        reviewService.resolve(answers.get(3).getAnswerId(), true);
        // exact match (1) + one year off (1) + accepted answer (1)
        assertThat(dailyQuizService.getPlayState(set.getId(), user.getEmail()).getResult().getScore()).isEqualTo(3);
    }

    @Test
    void theAttemptDetailShowsStatusScoreAndMaxSoAChangedDecisionIsVisible() {
        Question q = newQuestion("General", "Capital of Norway?", "Oslo");
        DailyQuizSet set = newSet(q);
        AppUser user = newUser();
        submit(set, user, "Oslo, i Norge");

        DailyQuizAttemptDetailDto waiting = detailFor(user, set);
        assertThat(waiting.getStatus()).isEqualTo("SUBMITTED");
        assertThat(waiting.getScore()).isNull();

        reviewService.resolve(waiting.getAnswers().get(0).getAnswerId(), true);
        DailyQuizAttemptDetailDto graded = detailFor(user, set);
        assertThat(graded.getStatus()).isEqualTo("GRADED");
        assertThat(graded.getScore()).isEqualTo(1);
        assertThat(graded.getMaxScore()).isEqualTo(1);
    }

    @Test
    void everyoneWhoSubmittedADayCanBeListedEvenOnceTheirAnswersAreAllDecided() {
        Question q = newQuestion("General", "Capital of Norway?", "Oslo");
        DailyQuizSet set = newSet(q);
        AppUser decided = newUser();
        AppUser waiting = newUser();
        AppUser opened = newUser();
        submit(set, decided, "Oslo, i Norge");
        submit(set, waiting, "Oslo, i Norge");
        reviewService.resolve(detailFor(decided, set).getAnswers().get(0).getAnswerId(), true);
        dailyQuizService.getPlayState(set.getId(), opened.getEmail()); // opened the quiz but never submitted

        assertThat(reviewService.listPendingAttempts()).as("fully decided -> gone from the waiting list")
                .noneMatch(p -> p.getPlayerName().equals(decided.getName()));

        var day = reviewService.listAttemptsForSet(set.getId());
        assertThat(day.getQuizDate()).isEqualTo(set.getQuizDate());
        assertThat(day.getAttempts()).extracting(a -> a.getPlayerName())
                .containsExactlyInAnyOrder(decided.getName(), waiting.getName());
        var decidedRow = day.getAttempts().stream().filter(a -> a.getPlayerName().equals(decided.getName())).findFirst().orElseThrow();
        assertThat(decidedRow.getStatus()).isEqualTo("GRADED");
        assertThat(decidedRow.getScore()).isEqualTo(1);
        assertThat(decidedRow.getMaxScore()).isEqualTo(1);
        assertThat(decidedRow.getPendingCount()).isZero();
        var waitingRow = day.getAttempts().stream().filter(a -> a.getPlayerName().equals(waiting.getName())).findFirst().orElseThrow();
        assertThat(waitingRow.getStatus()).isEqualTo("SUBMITTED");
        assertThat(waitingRow.getScore()).isNull();
        assertThat(waitingRow.getPendingCount()).isEqualTo(1);
    }
}
