package com.quizapp.dto;

import com.quizapp.model.Sport;

public class AthletePoolSummaryDto {
    private Long id;
    private String name;
    private Sport sport;
    private int memberCount;

    public AthletePoolSummaryDto(Long id, String name, Sport sport, int memberCount) {
        this.id = id;
        this.name = name;
        this.sport = sport;
        this.memberCount = memberCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Sport getSport() { return sport; }
    public int getMemberCount() { return memberCount; }
}
