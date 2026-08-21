package com.quizapp.repository;

import java.time.LocalDate;

public interface GridSummaryProjection {
    Long getId();
    String getTitle();
    String getSport();
    LocalDate getWeekStartDate();
    Long getEntryCount();
    Boolean getExcludedFromGridBattle();
}
