package com.quizapp.service;

import com.quizapp.dto.CategorySelectionDto;
import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import com.quizapp.dto.QuizGenerateRequest;
import com.quizapp.dto.ReplaceQuestionRequest;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuestionRepository questionRepository;

    public QuizService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Builds a quiz from one or more "N questions from category X" selections,
     * e.g. 3 Sport + 4 Film + 6 Literature, all in one language and optionally
     * restricted to a difficulty range (1 = easiest, 10 = hardest).
     */
    @Transactional(readOnly = true)
    public QuizDto generate(QuizGenerateRequest request) {
        List<QuestionDto> picked = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (CategorySelectionDto selection : request.getCategorySelections()) {
            List<Question> candidates = candidatePool(
                    request.getLanguage(), request.getMinDifficulty(), request.getMaxDifficulty(),
                    List.of(selection.getCategory()));
            Collections.shuffle(candidates);

            int wanted = selection.getNumberOfQuestions();
            int available = Math.min(wanted, candidates.size());

            picked.addAll(candidates.subList(0, available).stream()
                    .map(QuestionMapper::toDto)
                    .collect(Collectors.toList()));

            if (available < wanted) {
                warnings.add("Only found " + available + " of the " + wanted +
                        " requested \"" + selection.getCategory() + "\" question(s) in that language/difficulty range.");
            }
        }

        if (picked.isEmpty()) {
            throw new IllegalArgumentException(
                    "No questions in the bank match those categories/language/difficulty yet. " +
                            "Ask an admin to add some first.");
        }

        QuizDto quiz = new QuizDto();
        quiz.setTitle(request.getTitle() == null || request.getTitle().isBlank() ? "My Quiz" : request.getTitle());
        quiz.setQuestions(picked);
        quiz.setWarnings(warnings);
        return quiz;
    }

    @Transactional(readOnly = true)
    public QuestionDto replaceOne(ReplaceQuestionRequest request) {
        List<Long> excludeIds = request.getExcludeIds() == null ? Collections.emptyList() : request.getExcludeIds();
        Set<Long> excluded = Set.copyOf(excludeIds);

        List<Question> candidates = candidatePool(
                request.getLanguage(), request.getMinDifficulty(), request.getMaxDifficulty(),
                List.of(request.getCategory()));
        Collections.shuffle(candidates);

        return candidates.stream()
                .filter(q -> !excluded.contains(q.getId()))
                .findFirst()
                .map(QuestionMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No other \"" + request.getCategory() + "\" question is available to replace this one with."));
    }

    /**
     * Fetches every question in the given language (DB-side, cheap) then narrows by
     * category and difficulty range in memory - question banks here are small enough
     * that this is simpler and safer than a dynamic native IN-clause query.
     */
    private List<Question> candidatePool(Language language, Integer minDifficulty, Integer maxDifficulty,
                                          List<String> categories) {
        List<Question> byLanguage = questionRepository.findByLanguage(language);

        int min = minDifficulty == null ? 1 : minDifficulty;
        int max = maxDifficulty == null ? 10 : maxDifficulty;

        Set<String> wanted = categories.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toLowerCase())
                .collect(Collectors.toSet());

        return byLanguage.stream()
                .filter(q -> q.getDifficultyLevel() >= min && q.getDifficultyLevel() <= max)
                .filter(q -> wanted.isEmpty() || wanted.contains(q.getCategory().trim().toLowerCase()))
                .collect(Collectors.toList());
    }
}
