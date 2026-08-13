package com.quizapp.service;

import com.quizapp.model.Grid;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.GridRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GridAttemptCleanupService {

    private static final Logger log = LoggerFactory.getLogger(GridAttemptCleanupService.class);

    // Matches GridPlayService.findActive() (1 grid) + findArchive() (9 grids) -
    // the exact set a player sees on the weekly grid page. Computed the same
    // way here (grids with weekStartDate <= today, newest first) so the two
    // can never drift apart from each other over time.
    private static final int VISIBLE_GRID_COUNT = 10;

    private final GridRepository gridRepository;
    private final GridAttemptRepository gridAttemptRepository;

    public GridAttemptCleanupService(GridRepository gridRepository, GridAttemptRepository gridAttemptRepository) {
        this.gridRepository = gridRepository;
        this.gridAttemptRepository = gridAttemptRepository;
    }

    // Runs once a day - this is a weeks-scale concern, not an hours-scale one,
    // unlike RoomCleanupService's stale online rooms. Deletes attempt/score
    // data (not the grid itself - Grid Battle still needs the grid content) for
    // any grid that's fallen outside the 10 most recent, regardless of whether
    // that grid's attempts were completed or still in progress when it aged out.
    @Scheduled(fixedRate = 24L * 60 * 60 * 1000)
    @Transactional
    public void cleanup() {
        LocalDate today = LocalDate.now();
        List<Grid> visibleAndOlder = gridRepository.findByWeekStartDateLessThanEqualOrderByWeekStartDateDesc(today);
        if (visibleAndOlder.size() <= VISIBLE_GRID_COUNT) {
            return; // nothing has aged out of the visible window yet
        }

        List<Grid> agedOut = visibleAndOlder.subList(VISIBLE_GRID_COUNT, visibleAndOlder.size());
        int totalAttemptsRemoved = 0;
        for (Grid grid : agedOut) {
            int attemptCount = gridAttemptRepository.findByGrid_Id(grid.getId()).size();
            if (attemptCount == 0) continue; // already cleared on a previous run
            gridAttemptRepository.deleteSolvedEntriesByGridId(grid.getId());
            gridAttemptRepository.deleteOvertimeEntriesByGridId(grid.getId());
            gridAttemptRepository.deleteByGridId(grid.getId());
            totalAttemptsRemoved += attemptCount;
        }

        if (totalAttemptsRemoved > 0) {
            log.info("Grid attempt cleanup: removed {} attempt(s) across {} grid(s) older than the visible window",
                    totalAttemptsRemoved, agedOut.size());
        }
    }
}
