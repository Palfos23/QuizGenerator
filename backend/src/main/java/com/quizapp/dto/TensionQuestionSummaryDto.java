package com.quizapp.dto;

public class TensionQuestionSummaryDto {
    private Long id;
    private String title;
    private String mainCategory;
    private String answersCategory;
    private String source;
    private long safeCount;
    private long tensionCount;

    public TensionQuestionSummaryDto(Long id, String title, String mainCategory, String answersCategory,
                                      String source, long safeCount, long tensionCount) {
        this.id = id;
        this.title = title;
        this.mainCategory = mainCategory;
        this.answersCategory = answersCategory;
        this.source = source;
        this.safeCount = safeCount;
        this.tensionCount = tensionCount;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getMainCategory() { return mainCategory; }
    public String getAnswersCategory() { return answersCategory; }
    public String getSource() { return source; }
    public long getSafeCount() { return safeCount; }
    public long getTensionCount() { return tensionCount; }
}
