package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.FiveOhOneCategory;
import com.quizapp.model.FiveOhOneEntry;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.FiveOhOneCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FiveOhOneCategoryService {

    private final FiveOhOneCategoryRepository categoryRepository;
    private final AthleteRepository athleteRepository;

    public FiveOhOneCategoryService(FiveOhOneCategoryRepository categoryRepository, AthleteRepository athleteRepository) {
        this.categoryRepository = categoryRepository;
        this.athleteRepository = athleteRepository;
    }

    @Transactional(readOnly = true)
    public List<FiveOhOneCategorySummaryDto> findAllSummaries() {
        return categoryRepository.findAll().stream()
                .map(c -> new FiveOhOneCategorySummaryDto(c.getId(), c.getTitle(), c.getDescription(),
                        c.getEntries().size(), c.isCanExpire(), c.isEntireCategoryPool(), c.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    // Used by the admin editor - just the explicit, authored entries, exactly
    // as saved. The "entire category" pool (if any) is intentionally NOT
    // expanded here, same as Bullseye's admin builder never shows the whole
    // sport's roster either - see getOneForPlay for the play-time version.
    @Transactional(readOnly = true)
    public FiveOhOneCategoryDto getOne(Long id) {
        return toDto(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No 501 category found with id " + id)));
    }

    // Used by players (pass-and-play and online alike, both of which fetch a
    // category's entries through the public controller) - entries here are
    // whatever's actually guessable this round, explicit answers plus the
    // rest of the sport's roster when entireCategoryPool is on.
    @Transactional(readOnly = true)
    public FiveOhOneCategoryDto getOneForPlay(Long id) {
        FiveOhOneCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No 501 category found with id " + id));
        FiveOhOneCategoryDto dto = toDto(category);
        dto.setEntries(getEffectiveEntries(category));
        return dto;
    }

    // The pool-expanded entry list a round is actually played against -
    // shared by getOneForPlay (what the client searches/picks from) and
    // FiveOhOneOnlineService (server-side scoring/validation), so both agree
    // on exactly the same set. A pool-only row (no explicit value) gets a
    // negative id - the athlete's own id negated - which can never collide
    // with a real FiveOhOneEntry id (always positive), so a single "entryId"
    // number keeps working as the one thing a throw request needs to send,
    // whether it's an authored answer or a bare subject pick.
    @Transactional(readOnly = true)
    public List<FiveOhOneEntryDto> getEffectiveEntries(FiveOhOneCategory category) {
        List<FiveOhOneEntryDto> explicit = category.getEntries().stream()
                .map(e -> new FiveOhOneEntryDto(e.getId(), displayName(e), e.getValue(),
                        e.getAthlete() != null ? e.getAthlete().getId() : null))
                .collect(Collectors.toList());

        if (!category.isEntireCategoryPool() || category.getSport() == null || category.getSport().isBlank()) {
            return explicit;
        }

        Set<Long> linkedAthleteIds = explicit.stream()
                .map(FiveOhOneEntryDto::getAthleteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // Falls back to a name match for legacy entries with no athlete link
        // yet, so an old free-text "Erling Haaland" row doesn't also show up
        // a second time as an unlisted pool pick.
        Set<String> explicitNames = explicit.stream()
                .map(e -> e.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        List<FiveOhOneEntryDto> pool = athleteRepository.findBySport(category.getSport()).stream()
                .filter(a -> !linkedAthleteIds.contains(a.getId()) && !explicitNames.contains(a.getName().trim().toLowerCase()))
                .sorted(Comparator.comparing(Athlete::getName))
                .map(a -> new FiveOhOneEntryDto(-a.getId(), a.getName(), null, a.getId()))
                .collect(Collectors.toList());

        List<FiveOhOneEntryDto> combined = new ArrayList<>(explicit);
        combined.addAll(pool);
        return combined;
    }

    @Transactional
    public FiveOhOneCategoryDto create(FiveOhOneCategoryRequest request) {
        FiveOhOneCategory category = new FiveOhOneCategory();
        applyRequest(category, request);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public FiveOhOneCategoryDto update(Long id, FiveOhOneCategoryRequest request) {
        FiveOhOneCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No 501 category found with id " + id));
        applyRequest(category, request);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("No 501 category found with id " + id);
        }
        categoryRepository.deleteById(id);
    }

    private void applyRequest(FiveOhOneCategory category, FiveOhOneCategoryRequest request) {
        category.setTitle(request.getTitle());
        category.setDescription(request.getDescription());
        category.setSport(request.getSport());
        category.setCanExpire(request.isCanExpire());
        category.setEntireCategoryPool(request.isEntireCategoryPool());
        category.setUpdatedAt(java.time.Instant.now());

        // Reuse existing entries where possible rather than always creating fresh
        // rows - keeps saves fast for large categories and avoids needlessly
        // recreating rows that didn't actually change. Matched by linked subject
        // first (a rename changes the displayed name - see displayName() below -
        // without changing which entry that is), falling back to name only for
        // legacy free-text entries with no subject link.
        Map<Long, FiveOhOneEntry> existingByAthleteId = category.getEntries().stream()
                .filter(e -> e.getAthlete() != null)
                .collect(Collectors.toMap(e -> e.getAthlete().getId(), e -> e, (a, b) -> a));
        Map<String, FiveOhOneEntry> existingByName = category.getEntries().stream()
                .collect(Collectors.toMap(e -> e.getName().toLowerCase(), e -> e, (a, b) -> a));

        List<FiveOhOneEntry> entries = request.getEntries().stream()
                .map(input -> {
                    FiveOhOneEntry entry = input.getAthleteId() != null ? existingByAthleteId.get(input.getAthleteId()) : null;
                    if (entry == null) {
                        entry = existingByName.getOrDefault(input.getName().trim().toLowerCase(), new FiveOhOneEntry());
                    }
                    entry.setName(input.getName().trim());
                    entry.setValue(input.getValue() != null ? input.getValue() : 0);
                    entry.setAthlete(input.getAthleteId() != null
                            ? athleteRepository.findById(input.getAthleteId()).orElse(null)
                            : null);
                    return entry;
                })
                .collect(Collectors.toList());
        category.setEntries(entries);
    }

    // A linked entry's displayed name always follows its subject's current
    // name - the same live-reference behavior Bullseye/Grid/Lineup already
    // have - so renaming a Subject is reflected here immediately instead of
    // staying frozen at whatever name was typed/imported when the entry was
    // created. Only a legacy/free-text entry with no subject link falls back
    // to its own stored name.
    private static String displayName(FiveOhOneEntry e) {
        return e.getAthlete() != null ? e.getAthlete().getName() : e.getName();
    }

    static FiveOhOneCategoryDto toDto(FiveOhOneCategory c) {
        List<FiveOhOneEntryDto> entries = c.getEntries().stream()
                .map(e -> new FiveOhOneEntryDto(e.getId(), displayName(e), e.getValue(),
                        e.getAthlete() != null ? e.getAthlete().getId() : null))
                .collect(Collectors.toList());
        FiveOhOneCategoryDto dto = new FiveOhOneCategoryDto(c.getId(), c.getTitle(), c.getDescription(), entries);
        dto.setSport(c.getSport());
        dto.setEntireCategoryPool(c.isEntireCategoryPool());
        dto.setCanExpire(c.isCanExpire());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}
