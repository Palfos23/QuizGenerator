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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
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
    // enough content for the chosen player count, and (via getBattleRoundChoices)
    // for the round-start picker. Mirrors GridPlayService.findEligibleForGridBattle.
    @Transactional(readOnly = true)
    public List<BullseyeQuestionSummaryDto> findEligible() {
        return bullseyeQuestionRepository.findAllSummaries().stream()
                .filter(row -> !row.getExcludedFromBullseye())
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * For the "Random" round-start picker (mirrors GridPlayService
     * .getBattleRoundChoices / LineupPlayService.getBattleRoundChoices): a
     * small pool of candidate questions for the upcoming round, minus
     * whatever's already been played this game so a repeat never gets offered.
     */
    @Transactional(readOnly = true)
    public List<BullseyeQuestionSummaryDto> getBattleRoundChoices(int count, List<Long> excludeIds) {
        List<Long> ids = bullseyeQuestionRepository.findBattleEligibleIds().stream()
                .filter(id -> excludeIds == null || !excludeIds.contains(id))
                .collect(Collectors.toList());
        Collections.shuffle(ids);
        List<Long> sampled = ids.stream().limit(count).collect(Collectors.toList());
        if (sampled.isEmpty()) {
            return Collections.emptyList();
        }
        return bullseyeQuestionRepository.findSummariesByIdIn(sampled).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    private BullseyeQuestionSummaryDto toSummaryDto(BullseyeQuestionSummaryProjection row) {
        return new BullseyeQuestionSummaryDto(row.getId(), row.getTitle(), row.getSport(), row.getTargetValue(),
                row.getStatLabel(), row.getEntryCount().intValue(), row.getExcludedFromBullseye(),
                row.getEntireCategoryPool());
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
                question.getTargetValue(), question.getStatLabel(), entries);
    }

    // Tie-broken by id - same reasoning as GridPlayService.entrySortOrder: a Set
    // has no guaranteed iteration order, so without this the entry list could
    // visibly reshuffle between reads for no reason a player could see.
    private Comparator<BullseyeEntry> entrySortOrder() {
        return Comparator.comparingInt(BullseyeEntry::getOrderIndex).thenComparing(BullseyeEntry::getId);
    }
}
