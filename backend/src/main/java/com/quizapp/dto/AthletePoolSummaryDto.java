package com.quizapp.dto;


public class AthletePoolSummaryDto {
    private Long id;
    private String name;
    private String sport;
    private int memberCount;

    public AthletePoolSummaryDto(Long id, String name, String sport, int memberCount) {
        this.id = id;
        this.name = name;
        this.sport = sport;
        this.memberCount = memberCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public int getMemberCount() { return memberCount; }
}
