package com.quizapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quizapp.dto.DailyQuizPreviewDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.DailyQuizExclusion;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.DailyQuizExclusionRepository;
import com.quizapp.repository.DailyQuizQuestionUseRepository;
import com.quizapp.repository.DailyQuizSetRepository;
import com.quizapp.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// The admin's look at tomorrow's daily quiz: created early, hidden from players until its day, and editable -
// swap a question, pick one by hand, or rule out a whole category/label for the day.
@SpringBootTest
class DailyQuizPreviewTest {

    @Autowired
    private DailyQuizPreviewService preview;
    @Autowired
    private DailyQuizService quizService;
    @Autowired
    private DailyQuizSetRepository setRepository;
    @Autowired
    private DailyQuizQuestionUseRepository useRepository;
    @Autowired
    private DailyQuizExclusionRepository exclusionRepository;
    @Autowired
    private QuestionRepository questionRepository;

    private final LocalDate tomorrow = DailyQuizPreviewService.previewDate();

    @BeforeEach
    void freshTomorrow() {
        // The tests share one database; start each from "tomorrow's quiz not created yet".
        setRepository.findByQuizDate(tomorrow).ifPresent(setRepository::delete);
        useRepository.deleteAll(useRepository.findByQuizDate(tomorrow));
        exclusionRepository.deleteAll(exclusionRepository.findByQuizDate(tomorrow));
        seed("General", 25);
    }

    private List<Question> seed(String category, int count) {
        List<Question> out = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Question q = new Question();
            q.setQuestionText("Preview question " + System.nanoTime() + "-" + i);
            q.setCategory(category);
            q.setDifficultyLevel(5);
            q.setLanguage(Language.NO);
            q.setAnswer("Answer " + i);
            out.add(questionRepository.save(q));
        }
        return out;
    }

    private static List<Long> ids(DailyQuizPreviewDto dto) {
        return dto.questions().stream().map(DailyQuizPreviewDto.Item::id).collect(Collectors.toList());
    }

    @Test
    void previewCreatesTomorrowsQuizAndPlayersCannotOpenItYet() {
        DailyQuizPreviewDto dto = preview.get();

        assertThat(dto.date()).isEqualTo(tomorrow);
        assertThat(dto.questions()).isNotEmpty();
        assertThat(setRepository.findByQuizDate(tomorrow)).isPresent();
        // The same quiz comes back on the next look - it is created once.
        assertThat(ids(preview.get())).isEqualTo(ids(dto));
        // ...but players still get today's quiz, and asking for tomorrow's by id is told it doesn't exist.
        assertThat(quizService.getTodayForVisitor().getSetId()).isNotEqualTo(dto.setId());
        assertThatThrownBy(() -> quizService.getPlayState(dto.setId(), "someone@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void replacingAQuestionKeepsItsPlaceAndNeverRepeatsOneInTheQuiz() {
        DailyQuizPreviewDto before = preview.get();
        DailyQuizPreviewDto.Item target = before.questions().get(3);

        DailyQuizPreviewDto after = preview.replace(target.id());

        assertThat(after.questions()).hasSameSizeAs(before.questions());
        DailyQuizPreviewDto.Item now = after.questions().get(3);
        assertThat(now.id()).isNotEqualTo(target.id());
        assertThat(now.position()).isEqualTo(target.position());
        assertThat(ids(after)).doesNotHaveDuplicates();
        // The "used on this day" log follows the swap, so the old one is free again and the new one is counted.
        List<Long> logged = useRepository.findByQuizDate(tomorrow).stream().map(u -> u.getQuestionId()).collect(Collectors.toList());
        assertThat(logged).contains(now.id()).doesNotContain(target.id());
    }

    @Test
    void ruledOutCategoryIsRedrawnAndStaysOutForThatDay() {
        String category = "Utelatt " + System.nanoTime();
        List<Question> unwanted = seed(category, 40); // plenty to draw from
        DailyQuizPreviewDto first = preview.get();
        // make sure at least one is in the quiz, by swapping one in by hand (a normal slot)
        DailyQuizPreviewDto.Item normal = first.questions().stream().filter(i -> i.slot().equals("NORMAL")).findFirst().orElseThrow();
        Question notInQuiz = unwanted.stream().filter(q -> !ids(first).contains(q.getId())).findFirst().orElseThrow();
        DailyQuizPreviewDto withOne = preview.choose(normal.id(), notInQuiz.getId());
        assertThat(withOne.questions().stream().anyMatch(i -> category.equals(i.category()))).isTrue();

        DailyQuizPreviewDto after = preview.addExclusion("category", category.toLowerCase());

        assertThat(after.questions()).hasSameSizeAs(withOne.questions());
        assertThat(after.questions()).noneMatch(i -> category.equals(i.category()));
        assertThat(after.questions()).noneMatch(DailyQuizPreviewDto.Item::excluded);
        assertThat(after.exclusions()).extracting(DailyQuizPreviewDto.Exclusion::kind).contains(DailyQuizExclusion.CATEGORY);
        // Nor can one be picked by hand any more.
        Long other = unwanted.stream().map(Question::getId).filter(id -> !ids(after).contains(id)).findFirst().orElseThrow();
        assertThatThrownBy(() -> preview.choose(after.questions().get(2).id(), other))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(preview.candidates(after.questions().get(2).id(), category)).isEmpty();

        // Lifting the rule makes them pickable again.
        preview.removeExclusion("CATEGORY", category);
        assertThat(preview.candidates(after.questions().get(2).id(), category)).isNotEmpty();
    }

    @Test
    void aSpecificQuestionCanBePickedAndTheFixedSlotsKeepTheirKind() {
        Question wanted = seed("General", 1).get(0);
        DailyQuizPreviewDto dto = preview.get();
        DailyQuizPreviewDto.Item normal = dto.questions().stream().filter(i -> i.slot().equals("NORMAL")).findFirst().orElseThrow();

        DailyQuizPreviewDto after = preview.choose(normal.id(), wanted.getId());
        assertThat(ids(after)).contains(wanted.getId()).doesNotContain(normal.id());

        // Already in the quiz.
        assertThatThrownBy(() -> preview.choose(after.questions().get(0).id(), wanted.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        // A Year question can't take a normal slot's place by hand, nor the other way round.
        Question year = new Question();
        year.setQuestionText("Year question " + System.nanoTime());
        year.setCategory("Year");
        year.setDifficultyLevel(5);
        year.setLanguage(Language.NO);
        year.setAnswer("1999");
        year = questionRepository.save(year);
        Long yearId = year.getId();
        assertThatThrownBy(() -> preview.choose(wanted.getId(), yearId)).isInstanceOf(IllegalArgumentException.class);
    }
}
