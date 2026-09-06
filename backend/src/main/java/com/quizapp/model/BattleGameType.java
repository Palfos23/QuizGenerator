package com.quizapp.model;

// The "battle" game modes tracked for play-count statistics (see
// GamePlayEvent). Deliberately separate from RoomGameType, which is scoped
// to online rooms specifically and has no BULLSEYE, PENALTY_SHOOTOUT or
// FLASHBACK entry - none of those have an online room mode at all, only
// pass-and-play.
public enum BattleGameType {
    GRID_BATTLE,
    STARTING_XI_BATTLE,
    IMPOSTER,
    FIVE_O_ONE,
    BULLSEYE,
    PENALTY_SHOOTOUT,
    FLASHBACK
}
