package com.quizapp.repository;

public interface TensionQuestionSummaryProjection {
    Long getId();
    String getTitle();
    String getMainCategory();
    String getAnswersCategory();
    String getSource();
    Long getSafeCount();
    Long getTensionCount();
}
