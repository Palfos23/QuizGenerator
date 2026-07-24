package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.GridEntryViewDto;
import com.quizapp.dto.GridPlayStateDto;
import com.quizapp.dto.GridSummaryDto;
import com.quizapp.dto.GuessResultDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.Grid;
import com.quizapp.model.GridAttempt;
import com.quizapp.model.GridCandidate;
import com.quizapp.model.GridEntry;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.GridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GridPlayService {

    private final GridRepository gridRepository;
    private final GridAttemptRepository gridAttemptRepository;
    private final AppUserRepository appUserRepository;

    public GridPlayService(GridRepository gridRepository, GridAttemptRepository gridAttemptRepository,
                            AppUserRepository appUserRepository) {
        this.gridRepository = gridRepository;
        this.gridAttemptRepository = gridAttemptRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<com.quizapp.dto.GridScoreboardEntryDto> getScoreboard(Long gridId) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        int entryCount = grid.getEntries().size();

        return gridAttemptRepository.findByGrid_Id(gridId).stream()
                .filter(GridAttempt::isCompleted) // in-progress attempts aren't shown - they're still being played
                .map(a -> new com.quizapp.dto.GridScoreboardEntryDto(
                        a.getUser().getName(), a.getSolvedEntryIds().size(), entryCount,
                        a.isCompleted(), a.isOvertime()))
                .sorted((a, b) -> {
                    // clean solves rank ahead of overtime-assisted ones at the same score
                    int scoreCompare = b.getGuessedCount() - a.getGuessedCount();
                    if (scoreCompare != 0) return scoreCompare;
                    return Boolean.compare(a.isUsedOvertime(), b.isUsedOvertime());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GridSummaryDto> findActive(String userEmail) {
        LocalDate today = LocalDate.now();
        List<Grid> grids = gridRepository.findAll().stream()
                .filter(g -> isActive(g, today))
                .collect(Collectors.toList());
        return toSummariesWithStatus(grids, userEmail);
    }

    @Transactional(readOnly = true)
    public List<GridSummaryDto> findArchive(String userEmail) {
        LocalDate today = LocalDate.now();
        List<Grid> grids = gridRepository.findAll().stream()
                .filter(g -> !isActive(g, today) && g.getWeekStartDate().isBefore(today))
                .sorted((a, b) -> b.getWeekStartDate().compareTo(a.getWeekStartDate()))
                .collect(Collectors.toList());
        return toSummariesWithStatus(grids, userEmail);
    }

    /**
     * One query for all the user's attempts across every grid being listed, rather
     * than a separate query per grid - matters once there are a lot of grids.
     */
    private List<GridSummaryDto> toSummariesWithStatus(List<Grid> grids, String userEmail) {
        List<Long> gridIds = grids.stream().map(Grid::getId).collect(Collectors.toList());
        java.util.Map<Long, GridAttempt> attemptByGridId = gridIds.isEmpty()
                ? java.util.Map.of()
                : gridAttemptRepository.findByGrid_IdInAndUser_Email(gridIds, userEmail).stream()
                        .collect(Collectors.toMap(a -> a.getGrid().getId(), a -> a));

        return grids.stream().map(g -> {
            GridAttempt attempt = attemptByGridId.get(g.getId());
            String status = attempt == null ? "NOT_STARTED" : (attempt.isCompleted() ? "COMPLETED" : "IN_PROGRESS");
            // solvedEntryIds only ever contains genuinely-correct guesses (overtime
            // included) - never entries that were merely revealed - so this is the
            // same "found" count shown during actual play, not an inflated one.
            Integer guessedCount = attempt == null ? null : attempt.getSolvedEntryIds().size();
            return new GridSummaryDto(g.getId(), g.getTitle(), g.getSport(), g.getWeekStartDate(),
                    g.getEntries().size(), status, guessedCount);
        }).collect(Collectors.toList());
    }

    private boolean isActive(Grid grid, LocalDate today) {
        LocalDate start = grid.getWeekStartDate();
        LocalDate end = start.plusDays(6);
        return !today.isBefore(start) && !today.isAfter(end);
    }

    @Transactional
    public GridPlayStateDto getPlayState(Long gridId, String userEmail) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        GridAttempt attempt = findOrCreateAttempt(grid, userEmail);
        return toPlayStateDto(grid, attempt);
    }

    /** Search within just this grid's candidate pool - the point of the pool is to keep guessing scoped and relevant. */
    @Transactional(readOnly = true)
    public List<AthleteDto> searchCandidates(Long gridId, String nameContains) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        String term = nameContains == null ? "" : nameContains.trim().toLowerCase();
        return grid.getCandidates().stream()
                .map(GridCandidate::getAthlete)
                .filter(a -> term.isEmpty() || a.getName().toLowerCase().contains(term))
                .map(AthleteService::toDto)
                .limit(20)
                .collect(Collectors.toList());
    }

    @Transactional
    public GuessResultDto guess(Long gridId, String userEmail, Long athleteId) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        GridAttempt attempt = findOrCreateAttempt(grid, userEmail);

        if (attempt.isCompleted() && !attempt.isOvertime()) {
            throw new IllegalArgumentException("This grid is already finished. Start overtime to keep guessing.");
        }

        GuessResultDto result = new GuessResultDto();

        GridEntry matched = grid.getEntries().stream()
                .filter(e -> e.getAthlete().getId().equals(athleteId))
                .filter(e -> !attempt.getSolvedEntryIds().contains(e.getId()))
                .findFirst()
                .orElse(null);

        if (matched != null) {
            attempt.getSolvedEntryIds().add(matched.getId());
            if (attempt.isOvertime()) {
                attempt.getOvertimeSolvedEntryIds().add(matched.getId());
            }
            result.setCorrect(true);
            result.setEntry(new GridEntryViewDto(matched.getId(), matched.getHintLabel(), matched.getHintValue(),
                    true, true, attempt.isOvertime(), matched.getAthlete().getName(), matched.getAthlete().getPhotoUrl(),
                    logoUrl(matched), hintColor(matched)));

            boolean allSolved = attempt.getSolvedEntryIds().size() >= grid.getEntries().size();
            result.setAllSolved(allSolved);
            if (allSolved) {
                attempt.setCompleted(true);
            }
        } else {
            result.setCorrect(false);
            if (!attempt.isOvertime()) {
                attempt.setStrikesUsed(attempt.getStrikesUsed() + 1);
                if (attempt.getStrikesUsed() >= grid.getMaxStrikes()) {
                    attempt.setCompleted(true);
                }
            }
        }

        gridAttemptRepository.save(attempt);
        result.setStrikesUsed(attempt.getStrikesUsed());
        result.setMaxStrikes(grid.getMaxStrikes());
        result.setGameOver(attempt.isCompleted() && !result.isAllSolved());
        return result;
    }

    @Transactional
    public GridPlayStateDto enterOvertime(Long gridId, String userEmail) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        GridAttempt attempt = findOrCreateAttempt(grid, userEmail);
        if (!attempt.isCompleted()) {
            throw new IllegalArgumentException("Overtime is only available once the grid is finished.");
        }
        attempt.setOvertime(true);
        gridAttemptRepository.save(attempt);
        return toPlayStateDto(grid, attempt);
    }

    @Transactional
    public GridPlayStateDto reveal(Long gridId, String userEmail) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        GridAttempt attempt = findOrCreateAttempt(grid, userEmail);
        attempt.setCompleted(true);
        attempt.setRevealed(true);
        gridAttemptRepository.save(attempt);
        return toPlayStateDto(grid, attempt);
    }

    private GridAttempt findOrCreateAttempt(Grid grid, String userEmail) {
        return gridAttemptRepository.findByGrid_IdAndUser_Email(grid.getId(), userEmail)
                .orElseGet(() -> {
                    AppUser owner = appUserRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new ResourceNotFoundException("No account found for " + userEmail));
                    GridAttempt fresh = new GridAttempt();
                    fresh.setGrid(grid);
                    fresh.setUser(owner);
                    return gridAttemptRepository.save(fresh);
                });
    }

    private GridPlayStateDto toPlayStateDto(Grid grid, GridAttempt attempt) {
        GridPlayStateDto dto = new GridPlayStateDto();
        dto.setId(grid.getId());
        dto.setTitle(grid.getTitle());
        dto.setTheme(grid.getTheme());
        dto.setSport(grid.getSport());
        dto.setMaxStrikes(grid.getMaxStrikes());
        dto.setStrikesUsed(attempt.getStrikesUsed());
        dto.setCompleted(attempt.isCompleted());
        dto.setOvertime(attempt.isOvertime());
        dto.setRevealed(attempt.isRevealed());

        boolean revealAll = attempt.isRevealed();
        List<GridEntryViewDto> entries = grid.getEntries().stream()
                .sorted((a, b) -> b.getHintValue() - a.getHintValue())
                .map(e -> {
                    boolean guessedByUser = attempt.getSolvedEntryIds().contains(e.getId());
                    boolean solved = revealAll || guessedByUser;
                    boolean solvedInOvertime = attempt.getOvertimeSolvedEntryIds().contains(e.getId());
                    return new GridEntryViewDto(e.getId(), e.getHintLabel(), e.getHintValue(), solved, guessedByUser,
                            solvedInOvertime,
                            solved ? e.getAthlete().getName() : null,
                            solved ? e.getAthlete().getPhotoUrl() : null,
                            logoUrl(e), hintColor(e));
                })
                .collect(Collectors.toList());
        dto.setEntries(entries);
        return dto;
    }

    private String hintColor(GridEntry entry) {
        return entry.getClub() != null ? entry.getClub().getColor() : null;
    }

    // The logo is a hint shown alongside the label/value, visible whether or not the
    // tile is solved yet - unless the admin explicitly hid it for this entry, or no
    // club was set at all.
    private String logoUrl(GridEntry entry) {
        return (entry.isShowLogo() && entry.getClub() != null) ? entry.getClub().getLogoUrl() : null;
    }
}
