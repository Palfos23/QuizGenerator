package com.quizapp.model;

// The 5 "battle" game modes tracked for play-count statistics (see
// GamePlayEvent). Deliberately separate from RoomGameType, which is scoped
// to online rooms specifically and has no BULLSEYE entry - Bullseye has no
// online room mode at all, only pass-and-play.
public enum BattleGameType {
    GRID_BATTLE,
    STARTING_XI_BATTLE,
    IMPOSTER,
    FIVE_O_ONE,
    BULLSEYE
}
