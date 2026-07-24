package com.quizapp.service;

import com.quizapp.dto.CategorySelectionDto;
import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import com.quizapp.dto.QuizGenerateRequest;
import com.quizapp.dto.ReplaceQuestionRequest;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.model.SubmittedQuestion;
import com.quizapp.repository.QuestionRepository;
import com.quizapp.repository.SubmittedQuestionRepository;
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
    private final SubmittedQuestionRepository submittedQuestionRepository;

    public QuizService(QuestionRepository questionRepository, SubmittedQuestionRepository submittedQuestionRepository) {
        this.questionRepository = questionRepository;
        this.submittedQuestionRepository = submittedQuestionRepository;
    }

    /**
     * Builds a quiz from one or more "N questions from category X" selections,
     * e.g. 3 Sport + 4 Film + 6 Literature, all in one language and optionally
     * restricted to a difficulty range (1 = easiest, 10 = hardest).
     */
    @Transactional(readOnly = true)
    public QuizDto generate(QuizGenerateRequest request, String requestingUserEmail) {
        List<QuestionDto> picked = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (CategorySelectionDto selection : request.getCategorySelections()) {
            List<QuestionDto> candidates = candidatePool(
                    request.getLanguage(), request.getMinDifficulty(), request.getMaxDifficulty(),
                    selection.getCategory())
                    .stream().map(QuestionMapper::toDto).collect(Collectors.toList());

            if (request.isIncludeMySubmissions()) {
                candidates.addAll(personalCandidatePool(requestingUserEmail, request.getLanguage(),
                        request.getMinDifficulty(), request.getMaxDifficulty(), selection.getCategory()));
            }

            Collections.shuffle(candidates);

            int wanted = selection.getNumberOfQuestions();
            int available = Math.min(wanted, candidates.size());

            picked.addAll(candidates.subList(0, available));

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
        quiz.setLanguage(request.getLanguage());
        quiz.setQuestions(picked);
        quiz.setWarnings(warnings);
        return quiz;
    }

    /**
     * Fetches more questions to add to a quiz that's already being reviewed/edited -
     * used when a user wants to bump up a category's count, or add a category that
     * wasn't part of the original generation, without starting over.
     */
    @Transactional(readOnly = true)
    public List<QuestionDto> addQuestions(com.quizapp.dto.AddQuestionsRequest request) {
        List<Long> excludeIds = request.getExcludeIds() == null ? Collections.emptyList() : request.getExcludeIds();
        Set<Long> excluded = Set.copyOf(excludeIds);

        List<Question> candidates = candidatePool(
                request.getLanguage(), request.getMinDifficulty(), request.getMaxDifficulty(),
                request.getCategory());
        Collections.shuffle(candidates);

        return candidates.stream()
                .filter(q -> !excluded.contains(q.getId()))
                .limit(request.getCount())
                .map(QuestionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionDto replaceOne(ReplaceQuestionRequest request) {
        List<Long> excludeIds = request.getExcludeIds() == null ? Collections.emptyList() : request.getExcludeIds();
        Set<Long> excluded = Set.copyOf(excludeIds);

        List<Question> candidates = candidatePool(
                request.getLanguage(), request.getMinDifficulty(), request.getMaxDifficulty(),
                request.getCategory());
        Collections.shuffle(candidates);

        return candidates.stream()
                .filter(q -> !excluded.contains(q.getId()))
                .findFirst()
                .map(QuestionMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No other \"" + request.getCategory() + "\" question is available to replace this one with."));
    }

    /**
     * A requesting user's own submitted questions (any review status) are eligible
     * candidates for their own quizzes - that's what makes a rejected submission
     * still usable, just only for the person who wrote it. Approved ones are also
     * already in the shared bank by this point, so they'd otherwise show up twice;
     * excluding APPROVED here avoids that duplicate.
     */
    private List<QuestionDto> personalCandidatePool(String userEmail, Language language, Integer minDifficulty,
                                                      Integer maxDifficulty, String category) {
        if (userEmail == null) return Collections.emptyList();

        int min = minDifficulty == null ? 1 : minDifficulty;
        int max = maxDifficulty == null ? 10 : maxDifficulty;
        String wantedCategory = category == null ? "" : category.trim().toLowerCase();

        return submittedQuestionRepository.findBySubmittedBy_EmailAndLanguage(userEmail, language).stream()
                .filter(s -> s.getStatus() != com.quizapp.model.SubmissionStatus.APPROVED)
                .filter(s -> s.getDifficultyLevel() >= min && s.getDifficultyLevel() <= max)
                .filter(s -> wantedCategory.isEmpty() || s.getCategory().trim().toLowerCase().equals(wantedCategory))
                .map(QuizService::toQuestionDto)
                .collect(Collectors.toList());
    }

    // Negative ids guarantee no collision with real Question ids (which start at 1),
    // so discard/replace's excludeIds logic can't confuse a personal submission for
    // an unrelated shared question that happens to share the same numeric id.
    private static QuestionDto toQuestionDto(SubmittedQuestion s) {
        QuestionDto dto = new QuestionDto();
        dto.setId(-s.getId());
        dto.setQuestionText(s.getQuestionText());
        dto.setCategory(s.getCategory());
        dto.setDifficultyLevel(s.getDifficultyLevel());
        dto.setLanguage(s.getLanguage());
        dto.setAnswer(s.getAnswer());
        dto.setCouldChange(s.isCouldChange());
        return dto;
    }

    /**
     * Fetches every question in the given language (DB-side, cheap) then narrows by
     * category and difficulty range in memory - question banks here are small enough
     * that this is simpler and safer than a dynamic native IN-clause query.
     */
    /**
     * Delegates straight to the database-level query - see QuestionRepository.findCandidates
     * for why this matters (this runs on every generation, replace, and add-more call).
     */
    private List<Question> candidatePool(Language language, Integer minDifficulty, Integer maxDifficulty,
                                          String category) {
        int min = minDifficulty == null ? 1 : minDifficulty;
        int max = maxDifficulty == null ? 10 : maxDifficulty;
        return questionRepository.findCandidates(language, min, max, category);
    }
}
