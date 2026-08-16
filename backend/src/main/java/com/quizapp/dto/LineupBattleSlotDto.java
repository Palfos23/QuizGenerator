package com.quizapp.dto;

public class LineupBattleSlotDto {
    private Long id;
    private int slotIndex;
    private int shirtNumber;
    private boolean captain;
    private boolean solved;
    private String athleteName;
    private String athletePhotoUrl;
    private String solvedByName;

    public LineupBattleSlotDto(Long id, int slotIndex, int shirtNumber, boolean captain, boolean solved,
                                String athleteName, String athletePhotoUrl, String solvedByName) {
        this.id = id;
        this.slotIndex = slotIndex;
        this.shirtNumber = shirtNumber;
        this.captain = captain;
        this.solved = solved;
        this.athleteName = athleteName;
        this.athletePhotoUrl = athletePhotoUrl;
        this.solvedByName = solvedByName;
    }

    public Long getId() { return id; }
    public int getSlotIndex() { return slotIndex; }
    public int getShirtNumber() { return shirtNumber; }
    public boolean isCaptain() { return captain; }
    public boolean isSolved() { return solved; }
    public String getAthleteName() { return athleteName; }
    public String getAthletePhotoUrl() { return athletePhotoUrl; }
    public String getSolvedByName() { return solvedByName; }
}
