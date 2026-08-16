package com.quizapp.dto;

// One formation slot as shown to a player - mirrors GridEntryViewDto's "reveal
// only once solved" shape, simplified since a lineup has no hint label/value,
// club logo, or description reveal mode to carry.
public class LineupSlotDto {

    private Long id; // the LineupEntry id
    private int slotIndex;
    private int shirtNumber;
    private boolean captain;
    private boolean solved;
    private String athleteName; // null unless solved
    private String athletePhotoUrl; // null unless solved

    public LineupSlotDto(Long id, int slotIndex, int shirtNumber, boolean captain, boolean solved,
                          String athleteName, String athletePhotoUrl) {
        this.id = id;
        this.slotIndex = slotIndex;
        this.shirtNumber = shirtNumber;
        this.captain = captain;
        this.solved = solved;
        this.athleteName = athleteName;
        this.athletePhotoUrl = athletePhotoUrl;
    }

    public Long getId() { return id; }
    public int getSlotIndex() { return slotIndex; }
    public int getShirtNumber() { return shirtNumber; }
    public boolean isCaptain() { return captain; }
    public boolean isSolved() { return solved; }
    public String getAthleteName() { return athleteName; }
    public String getAthletePhotoUrl() { return athletePhotoUrl; }
}
