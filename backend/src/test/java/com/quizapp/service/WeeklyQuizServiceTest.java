package com.quizapp.service;

import com.quizapp.dto.WeeklyQuizPlayStateDto;
import com.quizapp.dto.WeeklyQuizSubmitRequest;
import com.quizapp.model.AppUser;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.model.WeeklyQuizSet;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.WeeklyQuizAnswerRepository;
import com.quizapp.repository.WeeklyQuizAttemptRepository;
import com.quizapp.repository.WeeklyQuizSetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class WeeklyQuizServiceTest {

    @Autowired
    private WeeklyQuizService weeklyQuizService;
    @Autowired
    private WeeklyQuizReviewService weeklyQuizReviewService;
    @Autowired
    private WeeklyQuizSetRepository weeklyQuizSetRepository;
    @Autowired
    private WeeklyQuizAttemptRepository weeklyQuizAttemptRepository;
    @Autowired
    private WeeklyQuizAnswerRepository weeklyQuizAnswerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser newUser() {
        // Both fields unique - tests in this class share one H2 database with
        // no per-test rollback, so a repeated name would make the admin
        // review queue's "answers for this player" filter ambiguous across tests.
        String suffix = String.valueOf(System.nanoTime());
        AppUser user = new AppUser();
        user.setEmail("player-" + suffix + "@example.com");
        user.setName("Test Player " + suffix);
        return appUserRepository.save(user);
    }

    private Question newQuestion(String text, String answer) {
        Question q = new Question();
        q.setQuestionText(text);
        q.setCategory("General");
        q.setDifficultyLevel(5);
        q.setLanguage(Language.NO);
        q.setAnswer(answer);
        return questionRepository.save(q);
    }

    private List<Question> seedQuestions(int count) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            questions.add(newQuestion("Question " + System.nanoTime() + "-" + i, "Answer" + i));
        }
        return questions;
    }

    @Test
    void exactCaseInsensitiveMatchAutoGrades() {
        // Seeds its own pool so "this week"'s set has plenty of Norwegian
        // questions to draw from, but doesn't assume it's THESE specific 15 -
        // other tests in this class share the same "current week" set (no
        // per-test rollback in this suite), so whichever test happens to run
        // first is the one that actually generates it. Look each returned
        // question's real answer up from the DB instead.
        seedQuestions(20);
        AppUser user = newUser();

        WeeklyQuizPlayStateDto play = weeklyQuizService.getPlayState(user.getEmail());
        assertThat(play.getAttemptStatus()).isEqualTo("IN_PROGRESS");
        assertThat(play.getQuestions()).hasSize(15);

        WeeklyQuizSubmitRequest request = new WeeklyQuizSubmitRequest();
        List<WeeklyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (WeeklyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            Question original = questionRepository.findById(q.getQuestionId()).orElseThrow();
            WeeklyQuizSubmitRequest.AnswerSubmission a = new WeeklyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            // exact match, but different case + surrounding whitespace
            a.setAnswerText("  " + original.getAnswer().toUpperCase() + "  ");
            answers.add(a);
        }
        request.setAnswers(answers);

        WeeklyQuizPlayStateDto result = weeklyQuizService.submitAnswers(user.getEmail(), request);

        assertThat(result.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(result.getResult().getScore()).isEqualTo(15);
        assertThat(result.getResult().getMaxScore()).isEqualTo(15);
        assertThat(result.getResult().getAnswers()).allMatch(a -> a.getVerdict().equals("CORRECT"));
    }

    @Test
    void blankAnswerAutoFailsWithoutNeedingReview() {
        seedQuestions(20);
        AppUser user = newUser();
        WeeklyQuizPlayStateDto play = weeklyQuizService.getPlayState(user.getEmail());

        WeeklyQuizSubmitRequest request = new WeeklyQuizSubmitRequest();
        List<WeeklyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (WeeklyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            WeeklyQuizSubmitRequest.AnswerSubmission a = new WeeklyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("   "); // skipped
            answers.add(a);
        }
        request.setAnswers(answers);

        WeeklyQuizPlayStateDto result = weeklyQuizService.submitAnswers(user.getEmail(), request);

        assertThat(result.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(result.getResult().getScore()).isEqualTo(0);
        assertThat(result.getResult().getAnswers()).allMatch(a -> a.getVerdict().equals("INCORRECT"));
    }

    @Test
    void nonExactAnswerStaysPendingUntilAdminResolvesIt() {
        seedQuestions(20);
        AppUser user = newUser();
        WeeklyQuizPlayStateDto play = weeklyQuizService.getPlayState(user.getEmail());

        WeeklyQuizSubmitRequest request = new WeeklyQuizSubmitRequest();
        List<WeeklyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (WeeklyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            WeeklyQuizSubmitRequest.AnswerSubmission a = new WeeklyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("Definitely not the right answer");
            answers.add(a);
        }
        request.setAnswers(answers);

        WeeklyQuizPlayStateDto submitted = weeklyQuizService.submitAnswers(user.getEmail(), request);
        assertThat(submitted.getAttemptStatus()).isEqualTo("SUBMITTED");
        assertThat(submitted.getResult()).isNull();

        // Score stays hidden entirely while anything is still pending.
        WeeklyQuizPlayStateDto stillWaiting = weeklyQuizService.getPlayState(user.getEmail());
        assertThat(stillWaiting.getAttemptStatus()).isEqualTo("SUBMITTED");

        var pending = weeklyQuizReviewService.listPending();
        assertThat(pending).hasSizeGreaterThanOrEqualTo(15);

        // Resolve all but the last one - still not graded.
        var forThisPlayer = pending.stream()
                .filter(p -> p.getPlayerName().equals(user.getName())).toList();
        for (int i = 0; i < forThisPlayer.size() - 1; i++) {
            weeklyQuizReviewService.resolve(forThisPlayer.get(i).getId(), i % 2 == 0);
        }
        assertThat(weeklyQuizService.getPlayState(user.getEmail()).getAttemptStatus()).isEqualTo("SUBMITTED");

        // Resolving the very last pending answer flips the whole attempt to GRADED.
        weeklyQuizReviewService.resolve(forThisPlayer.get(forThisPlayer.size() - 1).getId(), true);

        WeeklyQuizPlayStateDto graded = weeklyQuizService.getPlayState(user.getEmail());
        assertThat(graded.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(graded.getResult()).isNotNull();
        assertThat(graded.getResult().getMaxScore()).isEqualTo(15);
    }

    @Test
    void cannotSubmitTwice() {
        seedQuestions(20);
        AppUser user = newUser();
        WeeklyQuizPlayStateDto play = weeklyQuizService.getPlayState(user.getEmail());

        WeeklyQuizSubmitRequest request = new WeeklyQuizSubmitRequest();
        List<WeeklyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (WeeklyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            WeeklyQuizSubmitRequest.AnswerSubmission a = new WeeklyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("");
            answers.add(a);
        }
        request.setAnswers(answers);
        weeklyQuizService.submitAnswers(user.getEmail(), request);

        assertThatThrownBy(() -> weeklyQuizService.submitAnswers(user.getEmail(), request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void newSetAvoidsQuestionsUsedInRecentWeeks() {
        // Enough of a pool that exclusion is actually exercised rather than
        // being forced to reuse questions out of sheer scarcity.
        List<Question> pool = seedQuestions(40);
        List<Long> recentlyUsedIds = pool.stream().limit(15).map(Question::getId).toList();

        // A far-future week that no other test in this class could already
        // have touched - calling getOrCreateCurrentSet() here would only
        // ever race to create/reuse "this week", which every other test in
        // the class shares (no per-test transaction rollback in this suite).
        LocalDate futureWeek = LocalDate.now().plusYears(5)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        WeeklyQuizSet lastWeek = new WeeklyQuizSet();
        lastWeek.setWeekStartDate(futureWeek.minusWeeks(1));
        lastWeek.setQuestionIds(recentlyUsedIds);
        weeklyQuizSetRepository.save(lastWeek);

        WeeklyQuizSet generated = weeklyQuizService.generateSet(futureWeek);

        assertThat(generated.getQuestionIds()).hasSize(15);
        assertThat(generated.getQuestionIds()).noneMatch(recentlyUsedIds::contains);
    }
}
