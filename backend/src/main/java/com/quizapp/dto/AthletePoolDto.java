package com.quizapp.dto;


import java.util.List;

public class AthletePoolDto {
    private Long id;
    private String name;
    private String sport;
    private List<AthleteDto> members;

    public AthletePoolDto(Long id, String name, String sport, List<AthleteDto> members) {
        this.id = id;
        this.name = name;
        this.sport = sport;
        this.members = members;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public List<AthleteDto> getMembers() { return members; }
}
