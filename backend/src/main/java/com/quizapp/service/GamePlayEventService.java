package com.quizapp.service;

import com.quizapp.model.BattleGameType;
import com.quizapp.model.GamePlayEvent;
import com.quizapp.repository.GamePlayEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

// Records and tallies completed battle games (Grid Battle, Starting XI
// Battle, Imposter, 501, Bullseye, Penalty Shootout) for the admin Statistics
// page. See GamePlayEvent for why this exists as its own permanent table
// rather than counting GameRoom rows or something derived from existing state.
@Service
public class GamePlayEventService {

    private final GamePlayEventRepository repository;

    public GamePlayEventService(GamePlayEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(BattleGameType gameType) {
        GamePlayEvent event = new GamePlayEvent();
        event.setGameType(gameType.name());
        repository.save(event);
    }

    // Every BattleGameType is present, defaulting to 0 - so a mode with zero
    // plays still shows up as an empty bar rather than vanishing from the list.
    @Transactional(readOnly = true)
    public Map<BattleGameType, Long> countsByGameType() {
        Map<BattleGameType, Long> counts = new EnumMap<>(BattleGameType.class);
        for (BattleGameType type : BattleGameType.values()) {
            counts.put(type, 0L);
        }
        for (GamePlayEventRepository.GameTypeCount row : repository.countByGameType()) {
            try {
                counts.put(BattleGameType.valueOf(row.getGameType()), row.getTotal());
            } catch (IllegalArgumentException e) {
                // Ignore rows from a game type that's since been renamed/removed -
                // shouldn't happen since nothing ever writes an arbitrary string,
                // but this keeps a stale row from breaking the whole stats page.
            }
        }
        return counts;
    }
}
