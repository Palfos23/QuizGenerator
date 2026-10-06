package com.quizapp.service;

import com.quizapp.dto.DailyQuizPlayStateDto;
import com.quizapp.dto.DailyQuizResultDto;
import com.quizapp.dto.DailyQuizSetSummaryDto;
import com.quizapp.dto.DailyQuizSubmitRequest;
import com.quizapp.model.AppUser;
import com.quizapp.model.DailyQuizSet;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizSetRepository;
import com.quizapp.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DailyQuizServiceTest {

    @Autowired
    private DailyQuizService dailyQuizService;
    @Autowired
    private DailyQuizReviewService dailyQuizReviewService;
    @Autowired
    private DailyQuizSetRepository dailyQuizSetRepository;
    @Autowired
    private DailyQuizAttemptRepository dailyQuizAttemptRepository;
    @Autowired
    private DailyQuizAnswerRepository dailyQuizAnswerRepository;
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

    private DailyQuizSubmitRequest requestOf(Long questionId, String text) {
        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
        a.setQuestionId(questionId);
        a.setAnswerText(text);
        request.setAnswers(new ArrayList<>(List.of(a)));
        return request;
    }

    @Test
    void savedDraftComesBackOnTheNextLoadAndIsClearedBySubmit() {
        seedQuestions(20);
        AppUser user = newUser();
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();
        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(setId, user.getEmail());
        assertThat(play.getDraftAnswers()).isEmpty();

        Long q1 = play.getQuestions().get(0).getQuestionId();
        DailyQuizSubmitRequest draft = requestOf(q1, "  half-done answer ");
        DailyQuizSubmitRequest.AnswerSubmission blank = new DailyQuizSubmitRequest.AnswerSubmission();
        blank.setQuestionId(play.getQuestions().get(1).getQuestionId());
        blank.setAnswerText("   ");
        draft.getAnswers().add(blank);
        DailyQuizSubmitRequest.AnswerSubmission foreign = new DailyQuizSubmitRequest.AnswerSubmission();
        foreign.setQuestionId(-5L); // not one of this quiz's questions
        foreign.setAnswerText("nope");
        draft.getAnswers().add(foreign);
        dailyQuizService.saveDraft(setId, user.getEmail(), draft);

        DailyQuizPlayStateDto reloaded = dailyQuizService.getPlayState(setId, user.getEmail());
        assertThat(reloaded.getAttemptStatus()).isEqualTo("IN_PROGRESS");
        assertThat(reloaded.getDraftAnswers()).containsOnlyKeys(q1).containsEntry(q1, "half-done answer");

        // Saving again replaces the draft (here: emptied out).
        dailyQuizService.saveDraft(setId, user.getEmail(), requestOf(q1, ""));
        assertThat(dailyQuizService.getPlayState(setId, user.getEmail()).getDraftAnswers()).isEmpty();

        dailyQuizService.saveDraft(setId, user.getEmail(), requestOf(q1, "again"));
        dailyQuizService.submitAnswers(setId, user.getEmail(), requestOf(q1, "again"));
        assertThat(dailyQuizAttemptRepository.findBySet_IdAndUser_Email(setId, user.getEmail()).orElseThrow().getDraftAnswers()).isNull();
    }

    @Test
    void cannotSaveADraftAfterSubmitting() {
        seedQuestions(20);
        AppUser user = newUser();
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();
        Long q1 = dailyQuizService.getPlayState(setId, user.getEmail()).getQuestions().get(0).getQuestionId();
        dailyQuizService.submitAnswers(setId, user.getEmail(), requestOf(q1, "x"));

        assertThatThrownBy(() -> dailyQuizService.saveDraft(setId, user.getEmail(), requestOf(q1, "late")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exactCaseInsensitiveMatchAutoGrades() {
        // Seeds its own pool so "today"'s set has plenty of Norwegian
        // questions to draw from, but doesn't assume it's THESE specific 15 -
        // other tests in this class share the same "today" set (no per-test
        // rollback in this suite), so whichever test happens to run first is
        // the one that actually generates it. Look each returned question's
        // real answer up from the DB instead.
        seedQuestions(20);
        AppUser user = newUser();
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();

        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(setId, user.getEmail());
        assertThat(play.getAttemptStatus()).isEqualTo("IN_PROGRESS");
        assertThat(play.getQuestions()).hasSize(15);

        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (DailyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            Question original = questionRepository.findById(q.getQuestionId()).orElseThrow();
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            // exact match, but different case + surrounding whitespace
            a.setAnswerText("  " + original.getAnswer().toUpperCase() + "  ");
            answers.add(a);
        }
        request.setAnswers(answers);

        DailyQuizPlayStateDto result = dailyQuizService.submitAnswers(setId, user.getEmail(), request);

        assertThat(result.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(result.getResult().getScore()).isEqualTo(15);
        assertThat(result.getResult().getMaxScore()).isEqualTo(15);
        assertThat(result.getResult().getAnswers()).allMatch(a -> a.getVerdict().equals("CORRECT"));
        assertThat(result.getResult().getAnswers()).allMatch(a -> a.getCorrectAnswer() != null);
    }

    @Test
    void resultsPreserveTheSameQuestionOrderAsThePlayView() {
        // Regression: findByAttempt_Id had no ORDER BY, so Postgres could
        // (and did, in production) hand back a graded attempt's answers in a
        // different order than the numbered list the player actually
        // answered, scrambling question numbers on the reveal screen.
        seedQuestions(20);
        AppUser user = newUser();
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();
        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(setId, user.getEmail());
        List<String> playOrderQuestionTexts = play.getQuestions().stream()
                .map(DailyQuizPlayStateDto.QuestionDto::getText).toList();

        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (DailyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("");
            answers.add(a);
        }
        request.setAnswers(answers);

        DailyQuizPlayStateDto result = dailyQuizService.submitAnswers(setId, user.getEmail(), request);

        List<String> resultOrderQuestionTexts = result.getResult().getAnswers().stream()
                .map(DailyQuizResultDto.AnswerResultDto::getQuestionText).toList();
        assertThat(resultOrderQuestionTexts).containsExactlyElementsOf(playOrderQuestionTexts);
        for (int i = 0; i < result.getResult().getAnswers().size(); i++) {
            assertThat(result.getResult().getAnswers().get(i).getQuestionNumber()).isEqualTo(i + 1);
        }
    }

    @Test
    void blankAnswerAutoFailsWithoutNeedingReview() {
        seedQuestions(20);
        AppUser user = newUser();
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();
        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(setId, user.getEmail());

        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (DailyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("   "); // skipped
            answers.add(a);
        }
        request.setAnswers(answers);

        DailyQuizPlayStateDto result = dailyQuizService.submitAnswers(setId, user.getEmail(), request);

        assertThat(result.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(result.getResult().getScore()).isEqualTo(0);
        assertThat(result.getResult().getAnswers()).allMatch(a -> a.getVerdict().equals("INCORRECT"));
    }

    @Test
    void nonExactAnswerStaysPendingButYourOwnAnswerIsStillVisible() {
        seedQuestions(20);
        AppUser user = newUser();
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();
        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(setId, user.getEmail());

        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (DailyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("Definitely not the right answer");
            answers.add(a);
        }
        request.setAnswers(answers);

        DailyQuizPlayStateDto submitted = dailyQuizService.submitAnswers(setId, user.getEmail(), request);
        assertThat(submitted.getAttemptStatus()).isEqualTo("SUBMITTED");
        // Score is hidden while anything is still pending...
        assertThat(submitted.getResult()).isNotNull();
        assertThat(submitted.getResult().getScore()).isNull();
        // ...but your own submitted answers are still visible, marked PENDING,
        // with the correct answer withheld until it's resolved.
        assertThat(submitted.getResult().getAnswers()).hasSize(15);
        assertThat(submitted.getResult().getAnswers()).allMatch(a -> a.getVerdict().equals("PENDING"));
        assertThat(submitted.getResult().getAnswers()).allMatch(a -> a.getYourAnswer().equals("Definitely not the right answer"));
        assertThat(submitted.getResult().getAnswers()).allMatch(a -> a.getCorrectAnswer() == null);

        DailyQuizPlayStateDto stillWaiting = dailyQuizService.getPlayState(setId, user.getEmail());
        assertThat(stillWaiting.getAttemptStatus()).isEqualTo("SUBMITTED");

        var pendingAttempts = dailyQuizReviewService.listPendingAttempts();
        var thisPlayersPendingAttempt = pendingAttempts.stream()
                .filter(p -> p.getPlayerName().equals(user.getName())).findFirst().orElseThrow();
        assertThat(thisPlayersPendingAttempt.getPendingCount()).isEqualTo(15);

        var attemptDetail = dailyQuizReviewService.getAttemptDetail(thisPlayersPendingAttempt.getAttemptId());
        assertThat(attemptDetail.getPlayerName()).isEqualTo(user.getName());
        assertThat(attemptDetail.getAnswers()).hasSize(15);
        assertThat(attemptDetail.getAnswers()).allMatch(a -> a.getVerdict().equals("PENDING"));
        // Unlike the player-facing view, the admin always sees the correct answer.
        assertThat(attemptDetail.getAnswers()).allMatch(a -> a.getCorrectAnswer() != null);

        // Resolve all but the last one - still not graded.
        var forThisPlayer = attemptDetail.getAnswers();
        for (int i = 0; i < forThisPlayer.size() - 1; i++) {
            dailyQuizReviewService.resolve(forThisPlayer.get(i).getAnswerId(), i % 2 == 0);
        }
        assertThat(dailyQuizService.getPlayState(setId, user.getEmail()).getAttemptStatus()).isEqualTo("SUBMITTED");

        // Resolving the very last pending answer flips the whole attempt to GRADED.
        dailyQuizReviewService.resolve(forThisPlayer.get(forThisPlayer.size() - 1).getAnswerId(), true);

        DailyQuizPlayStateDto graded = dailyQuizService.getPlayState(setId, user.getEmail());
        assertThat(graded.getAttemptStatus()).isEqualTo("GRADED");
        assertThat(graded.getResult()).isNotNull();
        assertThat(graded.getResult().getScore()).isNotNull();
        assertThat(graded.getResult().getMaxScore()).isEqualTo(15);
        assertThat(graded.getResult().getAnswers()).noneMatch(a -> a.getVerdict().equals("PENDING"));
        assertThat(graded.getResult().getAnswers()).allMatch(a -> a.getCorrectAnswer() != null);
    }

    @Test
    void cannotSubmitTwice() {
        seedQuestions(20);
        AppUser user = newUser();
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();
        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(setId, user.getEmail());

        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (DailyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("");
            answers.add(a);
        }
        request.setAnswers(answers);
        dailyQuizService.submitAnswers(setId, user.getEmail(), request);

        assertThatThrownBy(() -> dailyQuizService.submitAnswers(setId, user.getEmail(), request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void newSetAvoidsQuestionsUsedInRecentDays() {
        // Enough of a pool that exclusion is actually exercised rather than
        // being forced to reuse questions out of sheer scarcity.
        List<Question> pool = seedQuestions(40);
        List<Long> recentlyUsedIds = pool.stream().limit(15).map(Question::getId).toList();

        // A far-future day that no other test in this class could already
        // have touched - calling getOrCreateCurrentSet() here would only
        // ever race to create/reuse "today", which every other test in the
        // class shares (no per-test transaction rollback in this suite).
        LocalDate futureDay = LocalDate.now().plusYears(5);
        DailyQuizSet yesterday = new DailyQuizSet();
        yesterday.setQuizDate(futureDay.minusDays(1));
        yesterday.setQuestionIds(recentlyUsedIds);
        dailyQuizSetRepository.save(yesterday);

        DailyQuizSet generated = dailyQuizService.generateSet(futureDay);

        assertThat(generated.getQuestionIds()).hasSize(15);
        assertThat(generated.getQuestionIds()).noneMatch(recentlyUsedIds::contains);
    }

    @Test
    void findActiveAlwaysReturnsExactlyToday() {
        AppUser user = newUser();
        List<DailyQuizSetSummaryDto> active = dailyQuizService.findActive(user.getEmail());
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo("NOT_STARTED");
    }

    @Test
    void scoreboardRanksGradedAttemptsAndRespectsOptOut() {
        seedQuestions(20);
        Long setId = dailyQuizService.getOrCreateCurrentSet().getId();

        AppUser winner = newUser();
        AppUser loser = newUser();
        gradeWithAllBlank(setId, winner.getEmail()); // score 0 - both start at 0
        gradeWithAllBlank(setId, loser.getEmail());

        // Winner opts out - still counted in the average, excluded from the list.
        dailyQuizService.setLeaderboardPreference(setId, winner.getEmail(), false);

        var board = dailyQuizService.getScoreboard(setId, loser.getEmail());
        assertThat(board.getEntries()).extracting("userName").doesNotContain(winner.getName());
        assertThat(board.getEntries()).extracting("userName").contains(loser.getName());

        // The opted-out player can still see their own row.
        var ownView = dailyQuizService.getScoreboard(setId, winner.getEmail());
        assertThat(ownView.getEntries()).extracting("userName").contains(winner.getName());
        assertThat(ownView.getYourLeaderboardPreference()).isFalse();
    }

    private void gradeWithAllBlank(Long setId, String email) {
        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(setId, email);
        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> answers = new ArrayList<>();
        for (DailyQuizPlayStateDto.QuestionDto q : play.getQuestions()) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(q.getQuestionId());
            a.setAnswerText("");
            answers.add(a);
        }
        request.setAnswers(answers);
        dailyQuizService.submitAnswers(setId, email, request);
    }

    @Test
    void cleanupDeletesAQuizOnceItIsSevenDaysOldButKeepsTheDayBefore() {
        // A quiz from 7 days ago (or older) is gone; a quiz from 6 days ago is the oldest one left -
        // so today and the 6 days before it, 7 quizzes in all.
        LocalDate today = LocalDate.now();
        DailyQuizSet keptOldest = new DailyQuizSet();
        keptOldest.setQuizDate(today.minusDays(6));
        keptOldest.setQuestionIds(List.of());
        dailyQuizSetRepository.save(keptOldest);

        DailyQuizSet deletedAtSeven = new DailyQuizSet();
        deletedAtSeven.setQuizDate(today.minusDays(7));
        deletedAtSeven.setQuestionIds(List.of());
        dailyQuizSetRepository.save(deletedAtSeven);

        DailyQuizSet deletedLongAgo = new DailyQuizSet();
        deletedLongAgo.setQuizDate(today.minusDays(12));
        deletedLongAgo.setQuestionIds(List.of());
        dailyQuizSetRepository.save(deletedLongAgo);

        dailyQuizService.deleteOldSets();

        assertThat(dailyQuizSetRepository.findById(keptOldest.getId())).isPresent();
        assertThat(dailyQuizSetRepository.findById(deletedAtSeven.getId())).isEmpty();
        assertThat(dailyQuizSetRepository.findById(deletedLongAgo.getId())).isEmpty();
    }

    private Question newLogoQuestion(String answer, boolean withPhoto) {
        Question q = new Question();
        q.setQuestionText("Hvilket selskap? " + System.nanoTime());
        q.setCategory("Logo");
        q.setDifficultyLevel(5);
        q.setLanguage(Language.NO);
        q.setAnswer(answer);
        if (withPhoto) q.setPhotoUrl("https://example.com/logo-" + System.nanoTime() + ".png");
        return questionRepository.save(q);
    }

    @Test
    void quizOpensWithExactlyOneLogoQuestionThatHasAPhoto() {
        seedQuestions(40);
        for (int i = 0; i < 5; i++) newLogoQuestion("Logo answer " + i, true);
        // A Logo question with no picture can never be the opener, and - like every
        // other Logo question - must not show up anywhere else in the quiz either.
        for (int i = 0; i < 3; i++) newLogoQuestion("Pictureless logo " + i, false);

        DailyQuizSet generated = dailyQuizService.generateSet(LocalDate.now().plusYears(6));

        assertThat(generated.getQuestionIds()).hasSize(15);
        List<Question> inOrder = generated.getQuestionIds().stream()
                .map(id -> questionRepository.findById(id).orElseThrow())
                .toList();
        assertThat(inOrder.get(0).getCategory()).isEqualTo("Logo");
        assertThat(inOrder.get(0).getPhotoUrl()).isNotBlank();
        assertThat(inOrder.stream().filter(q -> "Logo".equalsIgnoreCase(q.getCategory())).count())
                .as("exactly one Logo question per daily quiz").isEqualTo(1);
    }

    @Test
    void reusesALogoQuestionRatherThanDroppingThePictureRoundWhenAllWereRecentlyUsed() {
        seedQuestions(40);
        newLogoQuestion("Only logo", true);
        List<Long> everyLogoWithPhoto = questionRepository.findByLanguage(Language.NO).stream()
                .filter(q -> "Logo".equalsIgnoreCase(q.getCategory()) && q.getPhotoUrl() != null && !q.getPhotoUrl().isBlank())
                .map(Question::getId)
                .toList();

        LocalDate day = LocalDate.now().plusYears(7);
        DailyQuizSet yesterday = new DailyQuizSet();
        yesterday.setQuizDate(day.minusDays(1));
        yesterday.setQuestionIds(everyLogoWithPhoto);
        dailyQuizSetRepository.save(yesterday);

        DailyQuizSet generated = dailyQuizService.generateSet(day);

        Question opener = questionRepository.findById(generated.getQuestionIds().get(0)).orElseThrow();
        assertThat(opener.getCategory()).isEqualTo("Logo");
        assertThat(everyLogoWithPhoto).contains(opener.getId());
    }

    @Test
    void playStateCarriesThePhotoUrlForQuestionsThatHaveOne() {
        seedQuestions(40);
        Question logo = newLogoQuestion("Photo answer", true);
        AppUser user = newUser();
        // Hand-build a set so the logo is guaranteed to be in it (today's set is shared
        // with every other test in this class, so can't rely on what it contains).
        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(LocalDate.now().plusYears(8));
        set.setQuestionIds(List.of(logo.getId()));
        set = dailyQuizSetRepository.save(set);

        DailyQuizPlayStateDto play = dailyQuizService.getPlayState(set.getId(), user.getEmail());

        assertThat(play.getQuestions()).hasSize(1);
        assertThat(play.getQuestions().get(0).getPhotoUrl()).isEqualTo(logo.getPhotoUrl());
    }
}
