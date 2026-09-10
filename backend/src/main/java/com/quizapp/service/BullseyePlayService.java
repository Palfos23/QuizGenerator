package com.quizapp.service;

import com.quizapp.dto.BullseyeEntryViewDto;
import com.quizapp.dto.BullseyeQuestionSummaryDto;
import com.quizapp.dto.BullseyeRoundStateDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.BullseyeEntry;
import com.quizapp.model.BullseyeQuestion;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.BullseyeQuestionRepository;
import com.quizapp.repository.BullseyeQuestionSummaryProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BullseyePlayService {

    private final BullseyeQuestionRepository bullseyeQuestionRepository;
    private final AthleteRepository athleteRepository;

    public BullseyePlayService(BullseyeQuestionRepository bullseyeQuestionRepository,
                                AthleteRepository athleteRepository) {
        this.bullseyeQuestionRepository = bullseyeQuestionRepository;
        this.athleteRepository = athleteRepository;
    }

    // The pool a game draws from before it starts - used to check there's
    // enough content for the chosen player count (minus any excluded
    // categories, so that check is honest about what the round-choice picker
    // can actually offer), and (via getBattleRoundChoices) for the round-start
    // picker itself. Mirrors GridPlayService.findEligibleForGridBattle.
    @Transactional(readOnly = true)
    public List<BullseyeQuestionSummaryDto> findEligible(List<String> excludeCategories) {
        Set<String> excludedLower = excludeCategories == null ? Set.of() : excludeCategories.stream()
                .map(String::toLowerCase).collect(Collectors.toSet());
        return bullseyeQuestionRepository.findAllSummaries().stream()
                .filter(row -> !row.getExcludedFromBullseye())
                .filter(row -> !excludedLower.contains(row.getSport().toLowerCase()))
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    // For the round-start "exclude these categories" chip list.
    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return bullseyeQuestionRepository.findDistinctEligibleSports();
    }

    /**
     * For the "Random" round-start picker (mirrors GridPlayService
     * .getBattleRoundChoices / LineupPlayService.getBattleRoundChoices): a
     * small pool of candidate questions for the upcoming round, minus
     * whatever's already been played this game so a repeat never gets offered,
     * and minus any category the player asked to exclude entirely.
     *
     * Spreads the offered choices across different categories rather than
     * sampling uniformly at random - a plain random sample of, say, 3 from a
     * bank that's mostly football/geography would routinely offer "choose one
     * of 3 football questions" instead of a real choice. One question per
     * distinct category, in a random category order (and a random pick within
     * each), until `count` is reached; only tops back up with a second
     * question from an already-used category if there simply aren't enough
     * distinct categories left to fill the request.
     */
    @Transactional(readOnly = true)
    public List<BullseyeQuestionSummaryDto> getBattleRoundChoices(int count, List<Long> excludeIds, List<String> excludeCategories) {
        Set<String> excludedLower = excludeCategories == null ? Set.of() : excludeCategories.stream()
                .map(String::toLowerCase).collect(Collectors.toSet());

        List<BullseyeQuestionSummaryProjection> eligible = bullseyeQuestionRepository.findAllSummaries().stream()
                .filter(row -> !row.getExcludedFromBullseye())
                .filter(row -> excludeIds == null || !excludeIds.contains(row.getId()))
                .filter(row -> !excludedLower.contains(row.getSport().toLowerCase()))
                .collect(Collectors.toList());
        if (eligible.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<BullseyeQuestionSummaryProjection>> byCategory = eligible.stream()
                .collect(Collectors.groupingBy(BullseyeQuestionSummaryProjection::getSport));
        List<String> categories = new ArrayList<>(byCategory.keySet());
        Collections.shuffle(categories);
        byCategory.values().forEach(Collections::shuffle);

        List<BullseyeQuestionSummaryProjection> picked = new ArrayList<>();
        Set<Long> pickedIds = new HashSet<>();
        for (String category : categories) {
            if (picked.size() >= count) break;
            BullseyeQuestionSummaryProjection candidate = byCategory.get(category).get(0);
            picked.add(candidate);
            pickedIds.add(candidate.getId());
        }
        if (picked.size() < count) {
            List<BullseyeQuestionSummaryProjection> rest = eligible.stream()
                    .filter(row -> !pickedIds.contains(row.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(rest);
            for (BullseyeQuestionSummaryProjection row : rest) {
                if (picked.size() >= count) break;
                picked.add(row);
            }
        }
        Collections.shuffle(picked); // don't let the by-category build order leak into a fixed display order

        return picked.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    private BullseyeQuestionSummaryDto toSummaryDto(BullseyeQuestionSummaryProjection row) {
        return new BullseyeQuestionSummaryDto(row.getId(), row.getTitle(), row.getSport(), row.getTargetValue(),
                row.getStatLabel(), row.getEntryCount().intValue(), row.getExcludedFromBullseye(),
                row.getEntireCategoryPool(), row.getGroupDigits());
    }

    /**
     * Starting state for a chosen round - the full authored answer key, since
     * (unlike Grid) there's no secrecy to protect: the target number is shown
     * as plain text and every answer is revealed together at round end
     * regardless. Sent once per round so the client can match free-text
     * answers and resolve/tie-break locally without a round-trip per guess -
     * same reasoning TensionQuestionService already applies for its answer key.
     */
    @Transactional(readOnly = true)
    public BullseyeRoundStateDto getMultiplayerStartState(Long questionId) {
        BullseyeQuestion question = bullseyeQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("No question found with id " + questionId));

        List<BullseyeEntryViewDto> entries = question.getEntries().stream()
                .sorted(entrySortOrder())
                .map(e -> new BullseyeEntryViewDto(e.getAthlete().getId(), e.getAthlete().getName(), e.getStatValue()))
                .collect(Collectors.toList());

        // Auto pool: every other subject in this category is guessable too,
        // resolving to 0 if picked - queried live (like Grid's
        // entireCategoryPool) so a subject added to the category tomorrow is
        // immediately guessable here, no re-import needed.
        if (question.isEntireCategoryPool()) {
            Set<Long> alreadyListed = question.getEntries().stream()
                    .map(e -> e.getAthlete().getId())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<BullseyeEntryViewDto> rest = athleteRepository.findBySport(question.getSport()).stream()
                    .filter(a -> !alreadyListed.contains(a.getId()))
                    .sorted(Comparator.comparing(Athlete::getName))
                    .map(a -> new BullseyeEntryViewDto(a.getId(), a.getName(), null))
                    .collect(Collectors.toList());
            entries = new java.util.ArrayList<>(entries);
            entries.addAll(rest);
        }

        return new BullseyeRoundStateDto(question.getId(), question.getTitle(), question.getSport(),
                question.getTargetValue(), question.getStatLabel(), question.isGroupDigits(), question.getUpdatedAt(), entries);
    }

    // Tie-broken by id - same reasoning as GridPlayService.entrySortOrder: a Set
    // has no guaranteed iteration order, so without this the entry list could
    // visibly reshuffle between reads for no reason a player could see.
    private Comparator<BullseyeEntry> entrySortOrder() {
        return Comparator.comparingInt(BullseyeEntry::getOrderIndex).thenComparing(BullseyeEntry::getId);
    }
}
