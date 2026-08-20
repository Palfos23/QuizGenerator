package com.quizapp.repository;

import java.time.LocalDate;

public interface GridBattlePoolProjection {
    Long getId();
    String getTitle();
    String getSport();
    LocalDate getWeekStartDate();
    Long getEntryCount();
}
