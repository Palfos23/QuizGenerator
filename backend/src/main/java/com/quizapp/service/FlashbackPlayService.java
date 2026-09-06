package com.quizapp.service;

import com.quizapp.dto.FlashbackYearDto;
import com.quizapp.repository.FlashbackYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Entirely stateless, same reasoning as BullseyePlayService/PenaltyShootoutPlayService's
// class comments - no persisted per-game attempt. Unlike Penalty Shootout though
// (which genuinely withholds each kick's identity until guessed), a round-choice
// here already carries the full year + hint list - the same trust model
// TensionQuestionDto/BullseyeRoundStateDto use, see FlashbackYear's class
// comment. There's no separate "start" call: whichever of the round-choices
// gets picked already IS the full round state.
@Service
public class FlashbackPlayService {

    private final FlashbackYearRepository flashbackYearRepository;

    public FlashbackPlayService(FlashbackYearRepository flashbackYearRepository) {
        this.flashbackYearRepository = flashbackYearRepository;
    }

    /**
     * For the "pick your question" round-start screen: a small pool of
     * candidates for this specific round, excluding whatever's already been
     * played this game so a repeat never gets offered. No category to filter
     * by - every Flashback year is a historic event, there's nothing else to
     * narrow down to.
     */
    @Transactional(readOnly = true)
    public List<FlashbackYearDto> getRoundChoices(int count, List<Long> excludeIds) {
        List<Long> available = flashbackYearRepository.findEligibleIds().stream()
                .filter(id -> excludeIds == null || !excludeIds.contains(id))
                .collect(Collectors.toList());
        return loadRandomSubset(available, count);
    }

    // Shuffling and sampling at the ID level is cheap regardless of how many
    // years exist - only the small sampled subset actually gets its full
    // entity (and hint list) loaded.
    private List<FlashbackYearDto> loadRandomSubset(List<Long> ids, int count) {
        List<Long> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled);
        List<Long> sampled = shuffled.stream().limit(count).collect(Collectors.toList());
        return flashbackYearRepository.findAllById(sampled).stream()
                .map(FlashbackAdminService::toDto)
                .collect(Collectors.toList());
    }
}
