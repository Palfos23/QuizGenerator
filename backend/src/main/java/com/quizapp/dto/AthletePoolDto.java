package com.quizapp.dto;

import com.quizapp.model.Sport;

import java.util.List;

public class AthletePoolDto {
    private Long id;
    private String name;
    private Sport sport;
    private List<AthleteDto> members;

    public AthletePoolDto(Long id, String name, Sport sport, List<AthleteDto> members) {
        this.id = id;
        this.name = name;
        this.sport = sport;
        this.members = members;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Sport getSport() { return sport; }
    public List<AthleteDto> getMembers() { return members; }
}
