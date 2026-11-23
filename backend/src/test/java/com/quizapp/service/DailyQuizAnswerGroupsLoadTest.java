package com.quizapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.quizapp.dto.DailyQuizReviewDto;
import com.quizapp.dto.DailyQuizSubmitRequest;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// The admin's "Rett svar" page loads every distinct answer from the last week in one request. That request must not
// cost a database round trip per question / per player - with real traffic that is what made the page slow.
@SpringBootTest
class DailyQuizAnswerGroupsLoadTest {

    @Autowired private DailyQuizService dailyQuizService;
    @Autowired private DailyQuizReviewService reviewService;
    @Autowired private DailyQuizSetRepository setRepository;
    @Autowired private DailyQuizAttemptRepository attemptRepository;
    @Autowired private DailyQuizAnswerRepository answerRepository;
    @Autowired private DailyQuizResultRepository resultRepository;
    @Autowired private DailyQuizComplaintRepository complaintRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private static final AtomicInteger DAY_COUNTER = new AtomicInteger(5000);
    private final List<AppUser> users = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final List<DailyQuizSet> sets = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        List<Long> setIds = sets.stream().map(DailyQuizSet::getId).toList();
        List<DailyQuizAttempt> attempts = setIds.isEmpty() ? List.of() : attemptRepository.findBySet_IdIn(setIds);
        List<Long> attemptIds = attempts.stream().map(DailyQuizAttempt::getId).toList();
        if (!attemptIds.isEmpty()) {
            complaintRepository.deleteByAnswer_Attempt_IdIn(attemptIds);
            answerRepository.deleteByAttempt_IdIn(attemptIds);
        }
        attemptRepository.deleteAll(attempts);
        setRepository.deleteAll(sets);
        users.forEach(u -> resultRepository.deleteByUserId(u.getId()));
        questionRepository.deleteAll(questions);
        appUserRepository.deleteAll(users);
    }

    @Test
    void loadingTheAnswerGroupsDoesNotQueryPerQuestionOrPerPlayer() {
        int questionCount = 12, setCount = 3, playerCount = 25;
        for (int i = 0; i < questionCount; i++) {
            Question q = new Question();
            q.setQuestionText("Load test question " + System.nanoTime() + "-" + i);
            q.setCategory("General");
            q.setDifficultyLevel(5);
            q.setLanguage(Language.NO);
            q.setAnswer("Correct " + i);
            questions.add(questionRepository.save(q));
        }
        for (int d = 0; d < setCount; d++) {
            DailyQuizSet set = new DailyQuizSet();
            set.setQuizDate(LocalDate.now().plusYears(60).plusDays(DAY_COUNTER.incrementAndGet()));
            set.setQuestionIds(questions.stream().map(Question::getId).toList());
            sets.add(setRepository.save(set));
        }
        for (int p = 0; p < playerCount; p++) {
            AppUser u = new AppUser();
            u.setEmail("load-" + System.nanoTime() + "@example.com");
            u.setName("Load player " + System.nanoTime());
            u = appUserRepository.save(u);
            users.add(u);
            for (DailyQuizSet set : sets) {
                DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
                List<DailyQuizSubmitRequest.AnswerSubmission> list = new ArrayList<>();
                for (int i = 0; i < questionCount; i++) {
                    DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
                    a.setQuestionId(set.getQuestionIds().get(i));
                    a.setAnswerText(p % 3 == 0 ? "Correct " + i : "Guess " + (p % 5) + " for " + i);
                    list.add(a);
                }
                request.setAnswers(list);
                dailyQuizService.submitAnswers(set.getId(), u.getEmail(), request);
            }
        }

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        DailyQuizReviewDto.AnswerGroups groups = reviewService.listAnswerGroups();
        long statements = stats.getPrepareStatementCount();
        System.out.println("ANSWER-GROUPS statements: " + statements);

        // the days that were built are the ones created here (other tests' data may also be present)
        long ourDays = groups.days().stream().filter(day -> sets.stream().anyMatch(s -> s.getId().equals(day.setId()))).count();
        assertThat(ourDays).isEqualTo(setCount);
        // a handful of queries for the whole page - not ~one per question or per player
        assertThat(statements).isLessThanOrEqualTo(15);
    }
}
