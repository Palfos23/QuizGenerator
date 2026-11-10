package com.quizapp.service;

import com.quizapp.dto.DailyQuizPlayStateDto;
import com.quizapp.dto.DailyQuizResultDto;
import com.quizapp.dto.DailyQuizReviewDto;
import com.quizapp.dto.DailyQuizSubmitRequest;
import com.quizapp.model.*;
import com.quizapp.repository.*;
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

// Grading the daily quiz with less admin work: the same answer written differently is graded automatically, every
// distinct answer is decided once for everybody (and remembered), and players can ask for a second look at a wrong answer.
// Builds and removes its own data - the other daily quiz tests share this database.
@SpringBootTest
class DailyQuizAnswerReviewTest {

    @Autowired private DailyQuizService dailyQuizService;
    @Autowired private DailyQuizReviewService reviewService;
    @Autowired private DailyQuizComplaintService complaintService;
    @Autowired private AccountService accountService;
    @Autowired private DailyQuizSetRepository setRepository;
    @Autowired private DailyQuizAttemptRepository attemptRepository;
    @Autowired private DailyQuizAnswerRepository answerRepository;
    @Autowired private DailyQuizResultRepository resultRepository;
    @Autowired private DailyQuizComplaintRepository complaintRepository;
    @Autowired private DailyQuizAnswerRuleRepository ruleRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AppUserRepository appUserRepository;

    private static final AtomicInteger DAY_COUNTER = new AtomicInteger();
    private final List<AppUser> users = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final List<DailyQuizSet> sets = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        List<Long> setIds = sets.stream().map(DailyQuizSet::getId).toList();
        if (!setIds.isEmpty()) {
            List<DailyQuizAttempt> attempts = attemptRepository.findBySet_IdIn(setIds);
            List<Long> attemptIds = attempts.stream().map(DailyQuizAttempt::getId).toList();
            if (!attemptIds.isEmpty()) {
                complaintRepository.deleteByAnswer_Attempt_IdIn(attemptIds);
                answerRepository.deleteByAttempt_IdIn(attemptIds);
            }
            attemptRepository.deleteAll(attempts);
            setRepository.deleteAll(sets.stream().filter(s -> setRepository.existsById(s.getId())).toList());
        }
        ruleRepository.deleteAll(ruleRepository.findByQuestionIdIn(questions.stream().map(Question::getId).toList()));
        users.forEach(u -> resultRepository.deleteByUserId(u.getId()));
        questionRepository.deleteAll(questions.stream().filter(q -> questionRepository.existsById(q.getId())).toList());
        users.forEach(u -> { if (appUserRepository.existsById(u.getId())) appUserRepository.deleteById(u.getId()); });
        sets.clear();
        questions.clear();
        users.clear();
    }

    private AppUser user(String stem) {
        String suffix = String.valueOf(System.nanoTime());
        AppUser u = new AppUser();
        u.setEmail("grading-" + suffix + "@example.com");
        u.setName(stem + " " + suffix);
        u = appUserRepository.save(u);
        users.add(u);
        return u;
    }

    private Question question(String answer) {
        Question q = new Question();
        q.setQuestionText("Q " + System.nanoTime());
        q.setCategory("General");
        q.setDifficultyLevel(5);
        q.setLanguage(Language.NO);
        q.setAnswer(answer);
        q = questionRepository.save(q);
        questions.add(q);
        return q;
    }

    private DailyQuizSet set(Question... qs) {
        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(LocalDate.now().plusYears(50).plusDays(DAY_COUNTER.incrementAndGet()));
        set.setQuestionIds(java.util.Arrays.stream(qs).map(Question::getId).toList());
        set = setRepository.save(set);
        sets.add(set);
        return set;
    }

    /** Answers in the order of the set's questions. */
    private void submit(DailyQuizSet set, AppUser user, String... answers) {
        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        List<DailyQuizSubmitRequest.AnswerSubmission> list = new ArrayList<>();
        for (int i = 0; i < answers.length; i++) {
            DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
            a.setQuestionId(set.getQuestionIds().get(i));
            a.setAnswerText(answers[i]);
            list.add(a);
        }
        request.setAnswers(list);
        dailyQuizService.submitAnswers(set.getId(), user.getEmail(), request);
    }

    private DailyQuizAttempt attempt(DailyQuizSet set, AppUser user) {
        return attemptRepository.findBySet_IdAndUser_Email(set.getId(), user.getEmail()).orElseThrow();
    }

    private DailyQuizAnswer answerOf(DailyQuizSet set, AppUser user, int index) {
        return answerRepository.findByAttempt_IdOrderByIdAsc(attempt(set, user).getId()).get(index);
    }

    private DailyQuizResultDto.AnswerResultDto row(DailyQuizSet set, AppUser user, int index) {
        DailyQuizPlayStateDto state = dailyQuizService.getPlayState(set.getId(), user.getEmail());
        return state.getResult().getAnswers().get(index);
    }

    private DailyQuizReviewDto.QuestionGroups groupsFor(DailyQuizSet set, Question q) {
        return reviewService.listAnswerGroups().days().stream().filter(d -> d.setId().equals(set.getId()))
                .flatMap(d -> d.questions().stream()).filter(x -> x.questionId().equals(q.getId())).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ automatic grading

    @Test
    void theSameAnswerWrittenDifferentlyIsGradedAutomatically() {
        Question q = question("Ålesund");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        AppUser c = user("Cato");
        submit(set, a, "alesund");
        submit(set, b, "  ÅLESUND. ");
        submit(set, c, "Alesnd"); // a misspelling is not decided automatically

        assertThat(attempt(set, a).getStatus()).isEqualTo(DailyQuizAttemptStatus.GRADED);
        assertThat(attempt(set, a).getScore()).isEqualTo(1);
        assertThat(answerOf(set, a, 0).getDecidedBy()).isEqualTo(DailyQuizDecisionSource.NORMALIZED);
        assertThat(attempt(set, b).getScore()).isEqualTo(1);
        assertThat(attempt(set, c).getStatus()).isEqualTo(DailyQuizAttemptStatus.SUBMITTED);
        assertThat(answerOf(set, c, 0).getVerdict()).isEqualTo(DailyQuizAnswerVerdict.PENDING);
    }

    @Test
    void anExactMatchIsStillRecordedAsExact() {
        Question q = question("Oslo");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        submit(set, a, "oslo");
        assertThat(answerOf(set, a, 0).getDecidedBy()).isEqualTo(DailyQuizDecisionSource.EXACT);
    }

    // ------------------------------------------------------------------ one decision for everybody

    @Test
    void eachDistinctAnswerIsListedOnceWithHowManyWroteItAndTyposComeFirst() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        submit(set, user("A"), "Stavangr");
        submit(set, user("B"), "stavangr");
        submit(set, user("C"), "Bergen");
        submit(set, user("D"), "Bergen");
        submit(set, user("E"), "Bergen");
        submit(set, user("F"), "Stavanger"); // exact: nothing to judge, not listed

        DailyQuizReviewDto.QuestionGroups groups = groupsFor(set, q);

        assertThat(groups.correctAnswer()).isEqualTo("Stavanger");
        assertThat(groups.groups()).hasSize(2);
        DailyQuizReviewDto.Group typo = groups.groups().get(0);
        assertThat(typo.key()).isEqualTo("stavangr");
        assertThat(typo.count()).isEqualTo(2);
        assertThat(typo.pendingCount()).isEqualTo(2);
        assertThat(typo.likelyTypo()).isTrue();
        assertThat(groups.groups().get(1).key()).isEqualTo("bergen");
        assertThat(groups.groups().get(1).count()).isEqualTo(3);
        assertThat(groups.groups().get(1).likelyTypo()).isFalse();
    }

    @Test
    void decidingAnAnswerOnceGradesEveryPlayerWhoWroteItAndKeepsTheStandingsInStep() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        AppUser c = user("Cato");
        submit(set, a, "Stavangr");
        submit(set, b, "stavangr");
        submit(set, c, "Bergen");

        int changed = reviewService.decideGroup(q.getId(), "Stavangr", true, false);

        assertThat(changed).isEqualTo(2);
        for (AppUser u : List.of(a, b)) {
            assertThat(attempt(set, u).getStatus()).isEqualTo(DailyQuizAttemptStatus.GRADED);
            assertThat(attempt(set, u).getScore()).isEqualTo(1);
            assertThat(attempt(set, u).isReviewResultUnseen()).as("they are told their score is final").isTrue();
            assertThat(resultRepository.findByUserIdAndQuizDate(u.getId(), set.getQuizDate()).orElseThrow().getScore()).isEqualTo(1);
        }
        assertThat(attempt(set, c).getStatus()).as("a different answer is still waiting").isEqualTo(DailyQuizAttemptStatus.SUBMITTED);
        assertThat(ruleRepository.findByQuestionIdIn(List.of(q.getId()))).as("not remembered unless asked").isEmpty();
        // deciding again the other way fixes a mis-click, and the score follows
        reviewService.decideGroup(q.getId(), "stavangr", false, false);
        assertThat(attempt(set, a).getScore()).isZero();
    }

    @Test
    void aRememberedDecisionGradesTheSameAnswerAutomaticallyNextTime() {
        Question q = question("Stavanger");
        DailyQuizSet first = set(q);
        submit(first, user("Anna"), "Stavangr");
        submit(first, user("Bo"), "Bergen");

        reviewService.decideGroup(q.getId(), "Stavangr", true, true);
        reviewService.decideGroup(q.getId(), "Bergen", false, true);

        DailyQuizSet second = set(q);
        AppUser later = user("Senere");
        AppUser other = user("Annen");
        submit(second, later, "STAVANGR!");
        submit(second, other, "bergen");

        assertThat(attempt(second, later).getStatus()).isEqualTo(DailyQuizAttemptStatus.GRADED);
        assertThat(attempt(second, later).getScore()).isEqualTo(1);
        assertThat(answerOf(second, later, 0).getDecidedBy()).isEqualTo(DailyQuizDecisionSource.RULE);
        assertThat(attempt(second, other).getStatus()).isEqualTo(DailyQuizAttemptStatus.GRADED);
        assertThat(attempt(second, other).getScore()).as("remembered as wrong").isZero();
        // and the list shows what is remembered
        assertThat(groupsFor(first, q).groups()).extracting(DailyQuizReviewDto.Group::rule).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void theBulkButtonAcceptsOnlyWhatLooksLikeATypo() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser typo = user("Typo");
        AppUser other = user("Annen");
        submit(set, typo, "Stavnager");
        submit(set, other, "Bergen");

        int changed = reviewService.acceptLikelyTypos(set.getId());

        assertThat(changed).isEqualTo(1);
        assertThat(attempt(set, typo).getScore()).isEqualTo(1);
        assertThat(attempt(set, other).getStatus()).isEqualTo(DailyQuizAttemptStatus.SUBMITTED);
        assertThat(reviewService.acceptLikelyTypos(set.getId())).as("nothing left to accept").isZero();
    }

    @Test
    void yearQuestionsAndBlankAnswersAreNeverListedForReview() {
        Question q = question("Oslo");
        DailyQuizSet set = set(q);
        submit(set, user("Tom"), "");
        assertThat(reviewService.listAnswerGroups().days().stream().filter(d -> d.setId().equals(set.getId()))).isEmpty();
    }

    // ------------------------------------------------------------------ complaints

    @Test
    void aPlayerCanAskForAnotherLookAtAWrongAnswerOnlyOnAGradedQuiz() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        submit(set, a, "Stavangr");
        DailyQuizAnswer pending = answerOf(set, a, 0);

        assertThat(attempt(set, a).getStatus()).isEqualTo(DailyQuizAttemptStatus.SUBMITTED);
        assertThatThrownBy(() -> complaintService.complain(set.getId(), pending.getId(), a.getEmail(), "x")).hasMessageContaining("ferdig rettet");

        reviewService.decideGroup(q.getId(), "Stavangr", false, false);

        DailyQuizResultDto.AnswerResultDto before = row(set, a, 0);
        assertThat(before.isCanComplain()).isTrue();
        assertThat(before.getComplaintStatus()).isNull();

        DailyQuizPlayStateDto after = complaintService.complain(set.getId(), pending.getId(), a.getEmail(), "Jeg mente Stavanger, bare stavefeil");
        assertThat(after.getResult().getAnswers().get(0).getComplaintStatus()).isEqualTo("OPEN");
        assertThat(after.getResult().getAnswers().get(0).isCanComplain()).isFalse();
        assertThat(after.getResult().getComplaintsLeft()).isEqualTo(2);
        assertThatThrownBy(() -> complaintService.complain(set.getId(), pending.getId(), a.getEmail(), "igjen")).hasMessageContaining("allerede");
        assertThat(complaintService.listOpen()).anyMatch(c -> c.playerAnswer().equals("Stavangr") && c.comment().contains("stavefeil"));
    }

    @Test
    void youCannotComplainAboutOthersAnswersCorrectOnesOrBlanksAndOnlyThreeTimes() {
        Question q1 = question("A1"), q2 = question("B2"), q3 = question("C3"), q4 = question("D4"), q5 = question("E5");
        DailyQuizSet set = set(q1, q2, q3, q4, q5);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        submit(set, a, "feil1", "feil2", "feil3", "feil4", "E5");
        submit(set, b, "x", "x", "x", "x", "x");
        for (Question q : List.of(q1, q2, q3, q4)) {
            for (String t : List.of("feil1", "feil2", "feil3", "feil4")) reviewService.decideGroup(q.getId(), t, false, false);
        }
        List<DailyQuizAnswer> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attempt(set, a).getId());

        assertThatThrownBy(() -> complaintService.complain(set.getId(), answers.get(0).getId(), b.getEmail(), "")).hasMessageContaining("Fant ikke");
        assertThatThrownBy(() -> complaintService.complain(set.getId(), answers.get(4).getId(), a.getEmail(), "")).hasMessageContaining("riktig");
        for (int i = 0; i < 3; i++) complaintService.complain(set.getId(), answers.get(i).getId(), a.getEmail(), "");
        assertThatThrownBy(() -> complaintService.complain(set.getId(), answers.get(3).getId(), a.getEmail(), "")).hasMessageContaining("brukt opp");
        assertThatThrownBy(() -> complaintService.complain(set.getId(), answers.get(3).getId(), b.getEmail(), "")).hasMessageContaining("Fant ikke");
        assertThat(dailyQuizService.getPlayState(set.getId(), a.getEmail()).getResult().getAnswers().get(3).isCanComplain()).isFalse();
        assertThatThrownBy(() -> complaintService.complain(set.getId(), answers.get(0).getId(), a.getEmail(), "x".repeat(301))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void acceptingAComplaintMakesTheAnswerCorrectAndMovesTheScoreAndTheStandings() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        submit(set, a, "Stavangr");
        reviewService.decideGroup(q.getId(), "Stavangr", false, false);
        DailyQuizAnswer answer = answerOf(set, a, 0);
        complaintService.complain(set.getId(), answer.getId(), a.getEmail(), "");
        DailyQuizAttempt graded = attempt(set, a); // start from "nothing new to see", so the flag checked below is the complaint's
        graded.setReviewResultUnseen(false);
        attemptRepository.save(graded);
        Long complaintId = complaintRepository.findByAnswer_Id(answer.getId()).orElseThrow().getId();

        int changed = complaintService.accept(complaintId, false);

        assertThat(changed).isEqualTo(1);
        assertThat(answerOf(set, a, 0).getVerdict()).isEqualTo(DailyQuizAnswerVerdict.CORRECT);
        assertThat(answerOf(set, a, 0).getDecidedBy()).isEqualTo(DailyQuizDecisionSource.COMPLAINT);
        assertThat(attempt(set, a).getScore()).isEqualTo(1);
        assertThat(attempt(set, a).isReviewResultUnseen()).isTrue();
        assertThat(resultRepository.findByUserIdAndQuizDate(a.getId(), set.getQuizDate()).orElseThrow().getScore()).isEqualTo(1);
        assertThat(row(set, a, 0).getComplaintStatus()).isEqualTo("ACCEPTED");
        assertThat(complaintService.listOpen()).noneMatch(c -> c.id().equals(complaintId));
        assertThatThrownBy(() -> complaintService.accept(complaintId, false)).hasMessageContaining("allerede avgjort");
    }

    @Test
    void acceptingForEveryoneAlsoFixesTheOthersAndTheNextTime() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        submit(set, a, "Stavangr");
        submit(set, b, "stavangr");
        reviewService.decideGroup(q.getId(), "Stavangr", false, false);
        DailyQuizAnswer answer = answerOf(set, a, 0);
        complaintService.complain(set.getId(), answer.getId(), a.getEmail(), "");

        int changed = complaintService.accept(complaintRepository.findByAnswer_Id(answer.getId()).orElseThrow().getId(), true);

        assertThat(changed).isEqualTo(2);
        assertThat(attempt(set, a).getScore()).isEqualTo(1);
        assertThat(attempt(set, b).getScore()).isEqualTo(1);
        DailyQuizSet next = set(q);
        AppUser later = user("Senere");
        submit(next, later, "Stavangr");
        assertThat(attempt(next, later).getScore()).isEqualTo(1);
    }

    @Test
    void turningAComplaintDownKeepsTheScoreAndTellsThePlayerWhy() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        submit(set, a, "Bergen");
        reviewService.decideGroup(q.getId(), "Bergen", false, false);
        DailyQuizAnswer answer = answerOf(set, a, 0);
        complaintService.complain(set.getId(), answer.getId(), a.getEmail(), "Bergen er også en by");
        Long id = complaintRepository.findByAnswer_Id(answer.getId()).orElseThrow().getId();

        complaintService.reject(id, "Spørsmålet gjelder Rogaland");

        DailyQuizResultDto.AnswerResultDto row = row(set, a, 0);
        assertThat(row.getComplaintStatus()).isEqualTo("REJECTED");
        assertThat(row.getComplaintNote()).isEqualTo("Spørsmålet gjelder Rogaland");
        assertThat(row.isCanComplain()).isFalse();
        assertThat(attempt(set, a).getScore()).isZero();
        assertThat(answerOf(set, a, 0).getVerdict()).isEqualTo(DailyQuizAnswerVerdict.INCORRECT);
    }

    @Test
    void decidingTheAnswerCorrectSettlesAComplaintStillWaitingOnIt() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser a = user("Anna");
        submit(set, a, "Stavangr");
        reviewService.decideGroup(q.getId(), "Stavangr", false, false);
        DailyQuizAnswer answer = answerOf(set, a, 0);
        complaintService.complain(set.getId(), answer.getId(), a.getEmail(), "");

        reviewService.decideGroup(q.getId(), "Stavangr", true, false); // an admin fixes it in the per-answer list instead

        assertThat(row(set, a, 0).getComplaintStatus()).isEqualTo("ACCEPTED");
        assertThat(complaintService.listOpen()).noneMatch(c -> c.playerAnswer().equals("Stavangr") && c.playerName().equals(a.getName()));
    }

    // ------------------------------------------------------------------ housekeeping

    @Test
    void complaintsGoWithTheQuizAfterSevenDaysAndWithTheAccount() {
        Question q = question("Stavanger");
        DailyQuizSet set = set(q);
        AppUser gone = user("Slettes");
        AppUser stays = user("Blir");
        submit(set, gone, "Bergen");
        submit(set, stays, "Bergen");
        reviewService.decideGroup(q.getId(), "Bergen", false, false);
        complaintService.complain(set.getId(), answerOf(set, gone, 0).getId(), gone.getEmail(), "");
        complaintService.complain(set.getId(), answerOf(set, stays, 0).getId(), stays.getEmail(), "");
        assertThat(complaintRepository.count()).isGreaterThanOrEqualTo(2);

        accountService.deleteAccount(gone.getEmail(), null);
        assertThat(complaintRepository.findByAnswer_Attempt_Id(attempt(set, stays).getId())).hasSize(1);

        // the quiz reaches seven days old: it goes, and its complaints with it (no foreign key left dangling)
        long complaintsBefore = complaintRepository.count();
        set.setQuizDate(LocalDate.now().minusDays(8));
        setRepository.save(set);
        dailyQuizService.deleteOldSets();
        assertThat(setRepository.existsById(set.getId())).isFalse();
        assertThat(complaintRepository.count()).isEqualTo(complaintsBefore - 1);
    }
}
