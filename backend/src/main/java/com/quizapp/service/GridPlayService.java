package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.GridEntryViewDto;
import com.quizapp.dto.GridPlayStateDto;
import com.quizapp.dto.GridSummaryDto;
import com.quizapp.dto.GuessResultDto;
import com.quizapp.dto.MultiplayerGuessRequest;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.Athlete;
import com.quizapp.model.Grid;
import com.quizapp.model.GridAttempt;
import com.quizapp.model.GridCandidate;
import com.quizapp.model.GridEntry;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.AthleteDescriptionRepository;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.GridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GridPlayService {

    private final GridRepository gridRepository;
    private final GridAttemptRepository gridAttemptRepository;
    private final AppUserRepository appUserRepository;
    private final AthleteDescriptionRepository athleteDescriptionRepository;

    public GridPlayService(GridRepository gridRepository, GridAttemptRepository gridAttemptRepository,
                            AppUserRepository appUserRepository,
                            AthleteDescriptionRepository athleteDescriptionRepository) {
        this.gridRepository = gridRepository;
        this.gridAttemptRepository = gridAttemptRepository;
        this.appUserRepository = appUserRepository;
        this.athleteDescriptionRepository = athleteDescriptionRepository;
    }

    /**
     * Reveals every entry on a grid regardless of solved status - used when a
     * pass-and-play Grid Battle session ends because everyone ran out of lives, so
     * players can see what the remaining answers actually were. No persisted state
     * to check here (pass-and-play tracks nothing server-side), so this trusts the
     * client to only call it once the session has genuinely ended.
     */
    @Transactional(readOnly = true)
    public List<GridEntryViewDto> revealAllEntries(Long gridId) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        return grid.getEntries().stream()
                .sorted(entrySortOrder(grid))
                .map(e -> new GridEntryViewDto(e.getId(), e.getHintLabel(), e.getHintValue(), true, false, false,
                        e.getAthlete().getName(), resolvedPhotoUrl(e), logoUrl(e), hintColor(e), resolvedDescription(e)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void setLeaderboardPreference(Long gridId, String userEmail, boolean includeOnLeaderboard) {
        GridAttempt attempt = gridAttemptRepository.findByGrid_IdAndUser_Email(gridId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found for this grid."));
        attempt.setIncludeOnLeaderboard(includeOnLeaderboard);
        gridAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public com.quizapp.dto.GridScoreboardDto getScoreboard(Long gridId, String requestingUserEmail) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        int entryCount = grid.getEntries().size();

        List<GridAttempt> completedAttempts = gridAttemptRepository.findByGrid_Id(gridId).stream()
                .filter(GridAttempt::isCompleted) // in-progress attempts aren't shown - they're still being played
                .collect(Collectors.toList());

        List<com.quizapp.dto.GridScoreboardEntryDto> entries = completedAttempts.stream()
                // Opted-out players are still counted in the average (it's just a
                // number, doesn't reveal who they are) but excluded from the visible
                // ranked list - except your own row, which you can always see.
                .filter(a -> a.isIncludeOnLeaderboard() || a.getUser().getEmail().equals(requestingUserEmail))
                .map(a -> {
                    // Overtime solves don't count toward the competitive score - only
                    // what you found within your original lives does.
                    int overtimeCount = a.getOvertimeSolvedEntryIds().size();
                    int scoredSolves = a.getSolvedEntryIds().size() - overtimeCount;
                    return new com.quizapp.dto.GridScoreboardEntryDto(
                            a.getUser().getName(), scoredSolves, entryCount,
                            a.isCompleted(), a.isOvertime(), overtimeCount, a.getUser().getEmail().equals(requestingUserEmail));
                })
                .sorted((a, b) -> {
                    // clean solves rank ahead of overtime-assisted ones at the same score
                    int scoreCompare = b.getGuessedCount() - a.getGuessedCount();
                    if (scoreCompare != 0) return scoreCompare;
                    return Boolean.compare(a.isUsedOvertime(), b.isUsedOvertime());
                })
                .collect(Collectors.toList());

        double averageScore = completedAttempts.isEmpty() ? 0
                : completedAttempts.stream()
                        .mapToInt(a -> a.getSolvedEntryIds().size() - a.getOvertimeSolvedEntryIds().size())
                        .average().orElse(0);

        com.quizapp.dto.GridScoreboardDto dto = new com.quizapp.dto.GridScoreboardDto(entries, averageScore, entryCount);
        completedAttempts.stream()
                .filter(a -> a.getUser().getEmail().equals(requestingUserEmail))
                .findFirst()
                .ifPresent(a -> dto.setYourLeaderboardPreference(a.isIncludeOnLeaderboard()));
        return dto;
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
                .limit(9) // together with the 1 currently-active grid, matches the 10-grid
                          // window GridAttemptCleanupService also uses - keep these in sync
                .collect(Collectors.toList());
        return toSummariesWithStatus(grids, userEmail);
    }

    // Grids scheduled for a future week - not yet visible on the regular Weekly
    // Grid page (which only shows what's currently active or already past), but
    // still valid content for Grid Battle, where "this week's theme" pacing
    // doesn't apply the same way.
    @Transactional(readOnly = true)
    public List<GridSummaryDto> findFuture(String userEmail) {
        LocalDate today = LocalDate.now();
        List<Grid> grids = gridRepository.findAll().stream()
                .filter(g -> g.getWeekStartDate().isAfter(today))
                .sorted((a, b) -> a.getWeekStartDate().compareTo(b.getWeekStartDate()))
                .collect(Collectors.toList());
        return toSummariesWithStatus(grids, userEmail);
    }

    // Grid Battle draws from every grid regardless of date (past, current, or
    // future) since it doesn't have "this week's theme" pacing - the one thing
    // it excludes is a grid an admin has explicitly retired after duplicating
    // it forward with updated content.
    @Transactional(readOnly = true)
    public List<GridSummaryDto> findEligibleForGridBattle(String userEmail) {
        List<Grid> grids = gridRepository.findAll().stream()
                .filter(g -> !g.isExcludedFromGridBattle())
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
            GridSummaryDto dto = new GridSummaryDto(g.getId(), g.getTitle(), g.getSport(), g.getWeekStartDate(),
                    g.getEntries().size(), status, guessedCount);
            dto.setExcludedFromGridBattle(g.isExcludedFromGridBattle());
            return dto;
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
                .sorted(term.isEmpty() ? java.util.Comparator.comparing(Athlete::getName)
                        : java.util.Comparator.<Athlete>comparingInt(a -> matchRank(a.getName(), term))
                                .thenComparing(Athlete::getName))
                .map(AthleteService::toDto)
                .limit(5)
                .collect(Collectors.toList());
    }

    // Lower is more relevant - an exact match should never lose its spot in the
    // top 8 to a weaker "contains" match just because of how the candidates
    // happened to be ordered, which is especially likely once a pool is large
    // enough that many entries share a common substring.
    private int matchRank(String name, String term) {
        String lower = name.toLowerCase();
        if (lower.equals(term)) return 0;
        if (lower.startsWith(term)) return 1;
        return 2;
    }

    /**
     * Starting state for a multiplayer (local pass-and-play) game on this grid - all
     * tiles blank, nothing solved yet. Unlike the single-player /play endpoint, this
     * doesn't create or depend on a GridAttempt, since 2-4 local players share one
     * screen rather than each having their own persisted attempt.
     */
    @Transactional(readOnly = true)
    public GridPlayStateDto getMultiplayerStartState(Long gridId) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));

        GridPlayStateDto dto = new GridPlayStateDto();
        dto.setId(grid.getId());
        dto.setTitle(grid.getTitle());
        dto.setTheme(grid.getTheme());
        dto.setSport(grid.getSport());
        dto.setMaxStrikes(grid.getMaxStrikes());
        dto.setStrikesUsed(0);
        dto.setCompleted(false);
        dto.setOvertime(false);
        dto.setRevealed(false);
        dto.setEntries(grid.getEntries().stream()
                .sorted(entrySortOrder(grid))
                .map(e -> new GridEntryViewDto(e.getId(), e.getHintLabel(), e.getHintValue(), false, false, false,
                        null, grid.isRanked() ? null : resolvedPhotoUrl(e), logoUrl(e), hintColor(e), null))
                .collect(Collectors.toList()));
        return dto;
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getMultiplayerReveal(Long gridId) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));
        return grid.getEntries().stream()
                .collect(Collectors.toMap(GridEntry::getId, e -> e.getAthlete().getName()));
    }

    /**
     * Stateless guess check for multiplayer mode - same core matching logic as the
     * single-player guess(), but takes "already revealed" from the request instead
     * of a persisted GridAttempt, since there's no single account to attach one to.
     */
    @Transactional(readOnly = true)
    public GuessResultDto multiplayerGuess(Long gridId, MultiplayerGuessRequest request) {
        Grid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + gridId));

        Set<Long> revealed = request.getRevealedEntryIds() == null
                ? Collections.emptySet() : Set.copyOf(request.getRevealedEntryIds());

        GuessResultDto result = new GuessResultDto();
        GridEntry matched = grid.getEntries().stream()
                .filter(e -> e.getAthlete().getId().equals(request.getAthleteId()))
                .filter(e -> !revealed.contains(e.getId()))
                .findFirst()
                .orElse(null);

        if (matched != null) {
            result.setCorrect(true);
            result.setEntry(new GridEntryViewDto(matched.getId(), matched.getHintLabel(), matched.getHintValue(),
                    true, true, false, matched.getAthlete().getName(), resolvedPhotoUrl(matched),
                    logoUrl(matched), hintColor(matched), resolvedDescription(matched)));
            result.setAllSolved(revealed.size() + 1 >= grid.getEntries().size());
        } else {
            result.setCorrect(false);
            result.setAllSolved(false);
        }
        return result;
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
                    true, true, attempt.isOvertime(), matched.getAthlete().getName(), resolvedPhotoUrl(matched),
                    logoUrl(matched), hintColor(matched), resolvedDescription(matched)));

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
                .sorted(entrySortOrder(grid))
                .map(e -> {
                    boolean guessedByUser = attempt.getSolvedEntryIds().contains(e.getId());
                    boolean solved = revealAll || guessedByUser;
                    boolean solvedInOvertime = attempt.getOvertimeSolvedEntryIds().contains(e.getId());
                    boolean photoVisible = solved || !grid.isRanked();
                    return new GridEntryViewDto(e.getId(), e.getHintLabel(), e.getHintValue(), solved, guessedByUser,
                            solvedInOvertime,
                            solved ? e.getAthlete().getName() : null,
                            photoVisible ? resolvedPhotoUrl(e) : null,
                            logoUrl(e), hintColor(e), solved ? resolvedDescription(e) : null);
                })
                .collect(Collectors.toList());
        dto.setEntries(entries);
        return dto;
    }

    private java.util.Comparator<GridEntry> entrySortOrder(Grid grid) {
        // Tie-broken by id - without this, two entries with the exact same hint
        // value (e.g. two players tied on goals) have no defined relative order,
        // since entries is a Set with no guaranteed iteration order. That let their
        // relative position visibly shuffle between reads for no reason a player
        // could see, which is especially noticeable anywhere polling re-fetches
        // this repeatedly (online Grid Battle).
        if (!grid.isRanked()) {
            // No hint value to sort by at all - use the order the admin actually
            // built the entries in instead.
            return java.util.Comparator.comparingInt(GridEntry::getOrderIndex).thenComparing(GridEntry::getId);
        }
        java.util.Comparator<GridEntry> byValue = grid.isSortAscending()
                ? java.util.Comparator.comparingInt(GridEntry::getHintValue)
                : java.util.Comparator.comparingInt(GridEntry::getHintValue).reversed();
        return byValue.thenComparing(GridEntry::getId);
    }

    private String hintColor(GridEntry entry) {
        return entry.getClub() != null ? entry.getClub().getColor() : null;
    }

    // The logo is a hint shown alongside the label/value, visible whether or not the
    // tile is solved yet - unless the admin explicitly hid it for this entry, or no
    // club was set at all.
    private String logoUrl(GridEntry entry) {
        if (!entry.isShowLogo()) return null;
        if (entry.isUseOwnPhotoAsLogo()) return resolvedPhotoUrl(entry);
        return entry.getClub() != null ? entry.getClub().getLogoUrl() : null;
    }

    // The photo to actually show for this entry - a grid-specific override if
    // one was chosen, otherwise the athlete's own primary photo, same as
    // before this existed.
    private String resolvedPhotoUrl(GridEntry entry) {
        return entry.getSelectedPhoto() != null ? entry.getSelectedPhoto().getPhotoUrl() : entry.getAthlete().getPhotoUrl();
    }

    // Only meaningful when the grid's revealMode is DESCRIPTION - null
    // otherwise. Falls back to the athlete's first description if none was
    // specifically picked for this entry, and to null if the subject simply
    // has no descriptions at all (nothing honest to show).
    private String resolvedDescription(GridEntry entry) {
        if (entry.getGrid().getRevealMode() != Grid.RevealMode.DESCRIPTION) return null;
        if (entry.getSelectedDescription() != null) return entry.getSelectedDescription().getText();
        return athleteDescriptionRepository.findByAthlete_Id(entry.getAthlete().getId()).stream()
                .findFirst()
                .map(com.quizapp.model.AthleteDescription::getText)
                .orElse(null);
    }
}
