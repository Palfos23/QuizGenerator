package com.quizapp.service;

import com.quizapp.dto.FlashbackYearDto;
import com.quizapp.dto.FlashbackYearRequest;
import com.quizapp.dto.FlashbackYearSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.FlashbackYear;
import com.quizapp.repository.FlashbackYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlashbackAdminService {

    private static final int MAX_HINTS = 5;

    private final FlashbackYearRepository flashbackYearRepository;

    public FlashbackAdminService(FlashbackYearRepository flashbackYearRepository) {
        this.flashbackYearRepository = flashbackYearRepository;
    }

    @Transactional(readOnly = true)
    public List<FlashbackYearSummaryDto> findAll() {
        return flashbackYearRepository.findAllSummaries().stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .map(row -> new FlashbackYearSummaryDto(row.getId(), row.getTitle(),
                        row.getYear(), row.getHintCount().intValue(), row.getExcludedFromFlashback(),
                        row.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlashbackYearDto getOne(Long id) {
        return toDto(findYear(id));
    }

    @Transactional
    public FlashbackYearDto create(FlashbackYearRequest request) {
        FlashbackYear year = new FlashbackYear();
        applyRequest(year, request);
        return toDto(flashbackYearRepository.save(year));
    }

    @Transactional
    public FlashbackYearDto update(Long id, FlashbackYearRequest request) {
        FlashbackYear year = findYear(id);
        applyRequest(year, request);
        return toDto(flashbackYearRepository.save(year));
    }

    @Transactional
    public void delete(Long id) {
        if (!flashbackYearRepository.existsById(id)) {
            throw new ResourceNotFoundException("No Flashback year found with id " + id);
        }
        flashbackYearRepository.deleteById(id);
    }

    private void applyRequest(FlashbackYear year, FlashbackYearRequest request) {
        List<String> hints = request.getHints() == null ? List.of() : request.getHints().stream()
                .map(h -> h == null ? "" : h.trim())
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toList());
        if (hints.isEmpty()) {
            throw new IllegalArgumentException("Add at least 1 hint.");
        }
        if (hints.size() > MAX_HINTS) {
            throw new IllegalArgumentException("A year can have at most " + MAX_HINTS + " hints.");
        }

        year.setTitle(request.getTitle());
        year.setYear(request.getYear());
        year.setHints(hints);
        year.setExcludedFromFlashback(request.isExcludedFromFlashback());
        year.setUpdatedAt(Instant.now());
    }

    private FlashbackYear findYear(Long id) {
        return flashbackYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Flashback year found with id " + id));
    }

    static FlashbackYearDto toDto(FlashbackYear year) {
        FlashbackYearDto dto = new FlashbackYearDto();
        dto.setId(year.getId());
        dto.setTitle(year.getTitle());
        dto.setYear(year.getYear());
        dto.setHints(List.copyOf(year.getHints()));
        dto.setExcludedFromFlashback(year.isExcludedFromFlashback());
        dto.setUpdatedAt(year.getUpdatedAt());
        return dto;
    }
}
