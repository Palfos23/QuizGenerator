package com.quizapp.service;

import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// The category list backs the quiz generator's picker - and used to be built by loading every question in the
// bank. It now comes from a grouped query; this pins down that the result is the same as before.
@SpringBootTest
class QuestionCategoriesTest {

    @Autowired private QuestionService questionService;
    @Autowired private QuestionRepository questionRepository;

    private void question(String category, Language language) {
        Question q = new Question();
        q.setQuestionText("Q " + System.nanoTime());
        q.setCategory(category);
        q.setDifficultyLevel(5);
        q.setLanguage(language);
        q.setAnswer("A");
        questionRepository.save(q);
    }

    @Test
    void spellingsDifferingOnlyByCaseOrWhitespaceAreShownOnceUnderTheMostUsedOne() {
        String stem = "CatTest" + System.nanoTime();
        question(stem + "Geo", Language.DE);
        question(stem + "Geo", Language.DE);
        question(stem.toLowerCase() + "geo ", Language.DE); // same category, other casing + stray space - the minority spelling
        question(stem + "Sport", Language.DE);
        question(stem + "Other", Language.FR); // another language

        var german = questionService.findCategoriesForLanguage(Language.DE);

        assertThat(german).contains(stem + "Geo", stem + "Sport");
        assertThat(german).doesNotContain(stem.toLowerCase() + "geo", stem + "Other");
        assertThat(german.stream().filter(c -> c.equalsIgnoreCase(stem + "Geo"))).hasSize(1);
        assertThat(questionService.findCategoriesForLanguage(Language.FR)).contains(stem + "Other");
        assertThat(questionService.findAllCategories()).contains(stem + "Geo", stem + "Other");
    }
}
