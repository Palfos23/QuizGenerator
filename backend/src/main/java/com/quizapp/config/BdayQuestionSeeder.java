package com.quizapp.config;

import com.quizapp.model.BdayQuestion;
import com.quizapp.repository.BdayQuestionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * The first five statements of the birthday quiz, put in once when the question table is still empty so the
 * admin page has something to start from. After that the host owns the questions - nothing here ever overwrites them.
 */
@Component
public class BdayQuestionSeeder implements ApplicationRunner {

    private final BdayQuestionRepository repository;

    public BdayQuestionSeeder(BdayQuestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;
        add("Favorittlag: Middlesbrough", BdayQuestionCatalog.ERIK, 1);
        add("Har vært i Japan", BdayQuestionCatalog.PAL, 2);
        add("Har vært i militæret", BdayQuestionCatalog.ERIK, 3);
        add("Vart tatt ut på sonelaget i fotball", BdayQuestionCatalog.ERIK, 4);
        add("Har flest mål i følge fotball.no", BdayQuestionCatalog.PAL, 5);
    }

    private void add(String prompt, String correctOption, int sortOrder) {
        BdayQuestion q = new BdayQuestion();
        q.setType(BdayQuestion.Type.STATEMENT);
        q.setPrompt(prompt);
        q.setCorrectOption(correctOption);
        q.setSortOrder(sortOrder);
        repository.save(q);
    }
}
