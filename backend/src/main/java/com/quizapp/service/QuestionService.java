package com.quizapp.service;

import com.quizapp.dto.QuestionDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Question;
import com.quizapp.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> findAll() {
        return questionRepository.findAll().stream()
                .map(QuestionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionDto findById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No question found with id " + id));
        return QuestionMapper.toDto(question);
    }

    @Transactional(readOnly = true)
    public List<String> findAllCategories() {
        return questionRepository.findAllDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<String> findCategoriesForLanguage(com.quizapp.model.Language language) {
        return questionRepository.findByLanguage(language).stream()
                .map(Question::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Lets any logged-in user (not just admins) search the bank by text to hand-pick
     * specific questions into a quiz, rather than only drawing a random batch. Capped
     * at 30 results since this backs a live-typing search box, not a management table.
     */
    @Transactional(readOnly = true)
    public List<QuestionDto> searchForUser(com.quizapp.model.Language language, String term, String category) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        String wantedCategory = category == null ? "" : category.trim().toLowerCase();

        return questionRepository.findByLanguage(language).stream()
                .filter(q -> wantedCategory.isEmpty() || q.getCategory().trim().toLowerCase().equals(wantedCategory))
                .filter(q -> needle.isEmpty()
                        || q.getQuestionText().toLowerCase().contains(needle)
                        || q.getCategory().toLowerCase().contains(needle)
                        || q.getAnswer().toLowerCase().contains(needle))
                .limit(30)
                .map(QuestionMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Coverage breakdown per category/language combination - lets an admin spot thin
     * spots (e.g. "French Sports only has 2 questions") at a glance instead of finding
     * out when a quiz generation comes up short.
     */
    @Transactional(readOnly = true)
    public List<com.quizapp.dto.CategoryStatDto> getStats() {
        List<Question> all = questionRepository.findAll();

        Map<String, List<Question>> grouped = all.stream()
                .collect(Collectors.groupingBy(q -> q.getCategory() + "\u0000" + q.getLanguage()));

        return grouped.values().stream()
                .map(group -> {
                    Question first = group.get(0);
                    int min = group.stream().mapToInt(Question::getDifficultyLevel).min().orElse(0);
                    int max = group.stream().mapToInt(Question::getDifficultyLevel).max().orElse(0);
                    return new com.quizapp.dto.CategoryStatDto(
                            first.getCategory(), first.getLanguage(), group.size(), min, max);
                })
                .sorted(Comparator.comparing(com.quizapp.dto.CategoryStatDto::getCount)
                        .thenComparing(com.quizapp.dto.CategoryStatDto::getCategory))
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionDto create(QuestionDto dto) {
        Question saved = questionRepository.save(QuestionMapper.toEntity(dto));
        return QuestionMapper.toDto(saved);
    }

    @Transactional
    public QuestionDto update(Long id, QuestionDto dto) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No question found with id " + id));

        existing.setQuestionText(dto.getQuestionText());
        existing.setCategory(dto.getCategory());
        existing.setDifficultyLevel(dto.getDifficultyLevel());
        existing.setLanguage(dto.getLanguage());
        existing.setAnswer(dto.getAnswer());
        existing.setCouldChange(dto.isCouldChange());

        Question saved = questionRepository.save(existing);
        return QuestionMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("No question found with id " + id);
        }
        questionRepository.deleteById(id);
    }

    /**
     * Lets an admin populate an empty (or nearly empty) bank with a small set of sample
     * questions across all five languages, so there's something to generate a quiz from
     * immediately instead of facing a blank table. Safe to call more than once - it just
     * adds another copy of the sample set, so it's intended for a genuinely empty bank.
     */
    @Transactional
    public com.quizapp.dto.StarterPackResultDto addStarterPack() {
        List<Question> starterPack = com.quizapp.config.SampleQuestions.build();
        questionRepository.saveAll(starterPack);
        return new com.quizapp.dto.StarterPackResultDto(starterPack.size());
    }
}
