package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.LineupGuessRequest;
import com.quizapp.dto.LineupGuessResultDto;
import com.quizapp.dto.LineupPlayStateDto;
import com.quizapp.dto.LineupScoreboardDto;
import com.quizapp.dto.LineupScoreboardEntryDto;
import com.quizapp.dto.LineupSlotDto;
import com.quizapp.dto.LineupSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.Athlete;
import com.quizapp.model.Lineup;
import com.quizapp.model.LineupAttempt;
import com.quizapp.model.LineupCandidate;
import com.quizapp.model.LineupEntry;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.LineupAttemptRepository;
import com.quizapp.repository.LineupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Solo (weekly) Starting XI is persisted per-user via LineupAttempt, same
// pattern as GridAttempt for Grid. Local pass-and-play (MultiplayerLineupGame.vue)
// stays entirely stateless, same reasoning as Grid's multiplayer-start/-guess/-reveal
// trio: 2-4 players share one screen, so there's no single logged-in "attempt" to
// attach to - the client tracks its own guessed slots and reports them back on
// every call via the dedicated multiplayer* methods below. Online Starting XI
// Battle (LineupBattleOnlineService) is separate again and never touches this
// class at all.
@Service
public class LineupPlayService {

    private static final String DEFAULT_KIT_COLOR = "#d92332";
    private static final String DEFAULT_GK_KIT_COLOR = "#f2c230";

    private final LineupRepository lineupRepository;
    private final LineupAttemptRepository lineupAttemptRepository;
    private final AppUserRepository appUserRepository;
    private final AthleteRepository athleteRepository;

    public LineupPlayService(LineupRepository lineupRepository, LineupAttemptRepository lineupAttemptRepository,
                              AppUserRepository appUserRepository, AthleteRepository athleteRepository) {
        this.lineupRepository = lineupRepository;
        this.lineupAttemptRepository = lineupAttemptRepository;
        this.appUserRepository = appUserRepository;
        this.athleteRepository = athleteRepository;
    }

    @Transactional(readOnly = true)
    public List<LineupSummaryDto> findAll(String userEmail) {
        LocalDate today = LocalDate.now();
        List<Lineup> lineups = lineupRepository.findByWeekStartDateLessThanEqualOrderByWeekStartDateDescIdDesc(today);
        return toSummariesWithStatus(lineups, userEmail);
    }

    /**
     * For "Random" Starting XI Battle's round-start picker (mirrors
     * GridPlayService.getBattleRoundChoices / TensionQuestionService
     * .getRoundChoices): a small pool of candidate boards for the upcoming
     * round, drawn from the same battle-eligible pool
     * (LineupBattleOnlineService.initializeLineupSequence's old pool logic),
     * minus whatever's already been played this game.
     */
    @Transactional(readOnly = true)
    public List<LineupSummaryDto> getBattleRoundChoices(int count, List<Long> excludeIds) {
        List<Long> ids = lineupRepository.findAll().stream()
                .filter(l -> !l.isExcludedFromBattle())
                .map(Lineup::getId)
                .filter(id -> excludeIds == null || !excludeIds.contains(id))
                .collect(Collectors.toList());
        Collections.shuffle(ids);
        List<Long> sampled = ids.stream().limit(count).collect(Collectors.toList());
        return lineupRepository.findAllById(sampled).stream()
                .map(l -> new LineupSummaryDto(l.getId(), l.getTitle(), l.getCompetition(), l.getTeamName(),
                        l.getOpponentName(), l.getScoreFor(), l.getScoreAgainst(), l.getMatchDate(),
                        l.getWeekStartDate(), l.getFormation()))
                .collect(Collectors.toList());
    }

    /**
     * One query for all the user's attempts across every board being listed, rather
     * than a separate query per board - same reasoning as GridPlayService's version.
     */
    private List<LineupSummaryDto> toSummariesWithStatus(List<Lineup> lineups, String userEmail) {
        List<Long> lineupIds = lineups.stream().map(Lineup::getId).collect(Collectors.toList());
        java.util.Map<Long, LineupAttempt> attemptByLineupId = lineupIds.isEmpty()
                ? java.util.Map.of()
                : lineupAttemptRepository.findByLineup_IdInAndUser_Email(lineupIds, userEmail).stream()
                        .collect(Collectors.toMap(a -> a.getLineup().getId(), a -> a));

        return lineups.stream().map(l -> {
            LineupAttempt attempt = attemptByLineupId.get(l.getId());
            String status = attempt == null ? "NOT_STARTED" : (attempt.isCompleted() ? "COMPLETED" : "IN_PROGRESS");
            Integer guessedCount = attempt == null ? null : attempt.getSolvedEntryIds().size();
            LineupSummaryDto dto = new LineupSummaryDto(l.getId(), l.getTitle(), l.getCompetition(), l.getTeamName(),
                    l.getOpponentName(), l.getScoreFor(), l.getScoreAgainst(), l.getMatchDate(),
                    l.getWeekStartDate(), l.getFormation());
            dto.setStatus(status);
            dto.setGuessedCount(guessedCount);
            dto.setEntryCount(l.getEntries().size());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void setLeaderboardPreference(Long lineupId, String userEmail, boolean includeOnLeaderboard) {
        LineupAttempt attempt = lineupAttemptRepository.findByLineup_IdAndUser_Email(lineupId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found for this board."));
        attempt.setIncludeOnLeaderboard(includeOnLeaderboard);
        lineupAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public LineupScoreboardDto getScoreboard(Long lineupId, String requestingUserEmail) {
        Lineup lineup = findLineup(lineupId);
        int entryCount = lineup.getEntries().size();

        List<LineupAttempt> completedAttempts = lineupAttemptRepository.findByLineup_Id(lineupId).stream()
                .filter(LineupAttempt::isCompleted) // in-progress attempts aren't shown - they're still being played
                .collect(Collectors.toList());

        List<LineupScoreboardEntryDto> entries = completedAttempts.stream()
                .filter(a -> a.isIncludeOnLeaderboard() || a.getUser().getEmail().equals(requestingUserEmail))
                .map(a -> new LineupScoreboardEntryDto(a.getUser().getName(), a.getSolvedEntryIds().size(),
                        entryCount, a.isCompleted(), a.getUser().getEmail().equals(requestingUserEmail)))
                .sorted(Comparator.comparingInt(LineupScoreboardEntryDto::getGuessedCount).reversed())
                .collect(Collectors.toList());

        double averageScore = completedAttempts.isEmpty() ? 0
                : completedAttempts.stream().mapToInt(a -> a.getSolvedEntryIds().size()).average().orElse(0);

        LineupScoreboardDto dto = new LineupScoreboardDto(entries, averageScore, entryCount);
        completedAttempts.stream()
                .filter(a -> a.getUser().getEmail().equals(requestingUserEmail))
                .findFirst()
                .ifPresent(a -> dto.setYourLeaderboardPreference(a.isIncludeOnLeaderboard()));
        return dto;
    }

    /** Persisted starting/resume state for solo (weekly) play - creates a LineupAttempt on first visit. */
    @Transactional
    public LineupPlayStateDto getPlayState(Long lineupId, String userEmail) {
        Lineup lineup = findLineup(lineupId);
        LineupAttempt attempt = findOrCreateAttempt(lineup, userEmail);
        return toPlayStateDto(lineup, attempt);
    }

    /**
     * Search within just this board's candidate pool - the point of the pool is to
     * keep guessing scoped and relevant. When the board is in "entire category" mode
     * there's no stored pool to search at all - every Athlete tagged Football is
     * queried live instead.
     */
    @Transactional(readOnly = true)
    public List<AthleteDto> searchCandidates(Long lineupId, String nameContains) {
        Lineup lineup = findLineup(lineupId);
        String term = nameContains == null ? "" : nameContains.trim().toLowerCase();
        List<Athlete> pool = lineup.isEntireCategoryPool()
                ? (term.isEmpty()
                        ? athleteRepository.findBySport(Lineup.CATEGORY)
                        : athleteRepository.findBySportAndNameContainingIgnoreCase(Lineup.CATEGORY, term))
                : lineup.getCandidates().stream().map(LineupCandidate::getAthlete).collect(Collectors.toList());
        return pool.stream()
                .filter(a -> term.isEmpty() || a.getName().toLowerCase().contains(term))
                .sorted(term.isEmpty() ? Comparator.comparing(Athlete::getName)
                        : Comparator.<Athlete>comparingInt(a -> matchRank(a.getName(), term))
                                .thenComparing(Athlete::getName))
                .map(AthleteService::toDto)
                .limit(5)
                .collect(Collectors.toList());
    }

    private int matchRank(String name, String term) {
        String lower = name.toLowerCase();
        if (lower.equals(term)) return 0;
        if (lower.startsWith(term)) return 1;
        return 2;
    }

    /**
     * Starting state for a local pass-and-play multiplayer game on this board - all
     * shirts blank, nothing solved yet. Unlike getPlayState, this doesn't create or
     * depend on a LineupAttempt, since 2-4 local players share one screen rather
     * than each having their own persisted attempt.
     */
    @Transactional(readOnly = true)
    public LineupPlayStateDto getMultiplayerStartState(Long lineupId) {
        Lineup lineup = findLineup(lineupId);
        LineupPlayStateDto dto = baseDto(lineup);
        dto.setStrikesUsed(0);
        dto.setCompleted(false);
        dto.setRevealed(false);
        dto.setSlots(lineup.getEntries().stream()
                .sorted(slotOrder())
                .map(e -> new LineupSlotDto(e.getId(), e.getSlotIndex(), e.getShirtNumber(), e.isCaptain(),
                        false, false, null, null))
                .collect(Collectors.toList()));
        return dto;
    }

    /**
     * Stateless guess check for multiplayer mode - same core matching logic as the
     * persisted guess(), but takes "already revealed" from the request instead of a
     * persisted LineupAttempt, since there's no single account to attach one to.
     */
    @Transactional(readOnly = true)
    public LineupGuessResultDto multiplayerGuess(Long lineupId, LineupGuessRequest request) {
        Lineup lineup = findLineup(lineupId);
        Set<Long> revealed = request.getRevealedEntryIds() == null
                ? Collections.emptySet() : Set.copyOf(request.getRevealedEntryIds());

        LineupGuessResultDto result = new LineupGuessResultDto();
        LineupEntry matched = lineup.getEntries().stream()
                .filter(e -> !revealed.contains(e.getId()) && e.getAthlete().getId().equals(request.getAthleteId()))
                .findFirst().orElse(null);

        if (matched != null) {
            result.setCorrect(true);
            result.setSlot(new LineupSlotDto(matched.getId(), matched.getSlotIndex(), matched.getShirtNumber(),
                    matched.isCaptain(), true, true, matched.getAthlete().getName(), matched.getAthlete().getPhotoUrl()));
            result.setAllSolved(revealed.size() + 1 >= lineup.getEntries().size());
        } else {
            result.setCorrect(false);
            result.setAllSolved(false);
        }
        return result;
    }

    /** Every slot's answer, for showing what wasn't found once a pass-and-play round has ended. */
    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getMultiplayerReveal(Long lineupId) {
        Lineup lineup = findLineup(lineupId);
        return lineup.getEntries().stream()
                .collect(Collectors.toMap(LineupEntry::getId, e -> e.getAthlete().getName()));
    }

    @Transactional
    public LineupGuessResultDto guess(Long lineupId, String userEmail, Long athleteId) {
        Lineup lineup = findLineup(lineupId);
        LineupAttempt attempt = findOrCreateAttempt(lineup, userEmail);

        if (attempt.isCompleted()) {
            throw new IllegalArgumentException("This board is already finished.");
        }

        LineupGuessResultDto result = new LineupGuessResultDto();
        LineupEntry matched = lineup.getEntries().stream()
                .filter(e -> e.getAthlete().getId().equals(athleteId))
                .filter(e -> !attempt.getSolvedEntryIds().contains(e.getId()))
                .findFirst()
                .orElse(null);

        if (matched != null) {
            attempt.getSolvedEntryIds().add(matched.getId());
            result.setCorrect(true);
            result.setSlot(new LineupSlotDto(matched.getId(), matched.getSlotIndex(), matched.getShirtNumber(),
                    matched.isCaptain(), true, true, matched.getAthlete().getName(), matched.getAthlete().getPhotoUrl()));

            boolean allSolved = attempt.getSolvedEntryIds().size() >= lineup.getEntries().size();
            result.setAllSolved(allSolved);
            if (allSolved) {
                attempt.setCompleted(true);
            }
        } else {
            result.setCorrect(false);
            attempt.setStrikesUsed(attempt.getStrikesUsed() + 1);
            if (attempt.getStrikesUsed() >= lineup.getMaxStrikes()) {
                attempt.setCompleted(true);
            }
        }

        lineupAttemptRepository.save(attempt);
        result.setStrikesUsed(attempt.getStrikesUsed());
        result.setMaxStrikes(lineup.getMaxStrikes());
        result.setGameOver(attempt.isCompleted() && !result.isAllSolved());
        return result;
    }

    @Transactional
    public LineupPlayStateDto reveal(Long lineupId, String userEmail) {
        Lineup lineup = findLineup(lineupId);
        LineupAttempt attempt = findOrCreateAttempt(lineup, userEmail);
        attempt.setCompleted(true);
        attempt.setRevealed(true);
        lineupAttemptRepository.save(attempt);
        return toPlayStateDto(lineup, attempt);
    }

    private LineupAttempt findOrCreateAttempt(Lineup lineup, String userEmail) {
        return lineupAttemptRepository.findByLineup_IdAndUser_Email(lineup.getId(), userEmail)
                .orElseGet(() -> {
                    AppUser owner = appUserRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new ResourceNotFoundException("No account found for " + userEmail));
                    LineupAttempt fresh = new LineupAttempt();
                    fresh.setLineup(lineup);
                    fresh.setUser(owner);
                    return lineupAttemptRepository.save(fresh);
                });
    }

    private LineupPlayStateDto toPlayStateDto(Lineup lineup, LineupAttempt attempt) {
        LineupPlayStateDto dto = baseDto(lineup);
        dto.setStrikesUsed(attempt.getStrikesUsed());
        dto.setCompleted(attempt.isCompleted());
        dto.setRevealed(attempt.isRevealed());

        boolean revealAll = attempt.isRevealed();
        dto.setSlots(lineup.getEntries().stream()
                .sorted(slotOrder())
                .map(e -> {
                    boolean guessedByUser = attempt.getSolvedEntryIds().contains(e.getId());
                    boolean solved = revealAll || guessedByUser;
                    return new LineupSlotDto(e.getId(), e.getSlotIndex(), e.getShirtNumber(), e.isCaptain(),
                            solved, guessedByUser,
                            solved ? e.getAthlete().getName() : null,
                            solved ? e.getAthlete().getPhotoUrl() : null);
                })
                .collect(Collectors.toList()));
        return dto;
    }

    private LineupPlayStateDto baseDto(Lineup lineup) {
        LineupPlayStateDto dto = new LineupPlayStateDto();
        dto.setId(lineup.getId());
        dto.setTitle(lineup.getTitle());
        dto.setCompetition(lineup.getCompetition());
        dto.setTeamName(lineup.getTeamName());
        dto.setTeamCrestUrl(lineup.getTeamCrestUrl());
        dto.setOpponentName(lineup.getOpponentName());
        dto.setOpponentCrestUrl(lineup.getOpponentCrestUrl());
        dto.setScoreFor(lineup.getScoreFor());
        dto.setScoreAgainst(lineup.getScoreAgainst());
        dto.setMatchDate(lineup.getMatchDate());
        dto.setFormation(lineup.getFormation());
        dto.setMaxStrikes(lineup.getMaxStrikes());
        dto.setKitColor(lineup.getKitColor() != null ? lineup.getKitColor() : DEFAULT_KIT_COLOR);
        dto.setGoalkeeperKitColor(lineup.getGoalkeeperKitColor() != null ? lineup.getGoalkeeperKitColor() : DEFAULT_GK_KIT_COLOR);
        return dto;
    }

    private Lineup findLineup(Long id) {
        return lineupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Starting XI board found with id " + id));
    }

    private Comparator<LineupEntry> slotOrder() {
        return Comparator.comparingInt(LineupEntry::getSlotIndex);
    }
}
