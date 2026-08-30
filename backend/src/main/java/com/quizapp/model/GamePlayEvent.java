package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

// One row per completed battle game (Grid Battle, Starting XI Battle, Imposter,
// 501, Bullseye) - online or pass-and-play. Deliberately separate from
// GameRoom: online rooms get purged by RoomCleanupService ~24h after
// finishing, so they can't answer "how many games have ever been played",
// and pass-and-play games never persist any room at all. This table is
// insert-only and never cleaned up, so a simple count-by-type is a reliable
// lifetime total. See GamePlayEventService for where rows get written.
@Entity
@Table(name = "game_play_events")
public class GamePlayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One of BattleGameType's names - kept as a plain string (not
    // @Enumerated straight onto BattleGameType) for the same reason
    // GameRoom.gameType now uses an explicit varchar column: adding a new
    // game type later must never require widening a baked-in CHECK
    // constraint under ddl-auto=update.
    @Column(nullable = false, length = 40)
    private String gameType;

    @Column(nullable = false)
    private Instant playedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Instant playedAt) {
        this.playedAt = playedAt;
    }
}
