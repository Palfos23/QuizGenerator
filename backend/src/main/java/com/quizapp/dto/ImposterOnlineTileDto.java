package com.quizapp.dto;

public class ImposterOnlineTileDto {
    private Long id;
    private String athleteName;
    private String photoUrl;
    private String logoUrl;
    private boolean flipped;
    private Boolean imposter; // null until flipped
    private String flippedByName; // null until flipped

    public ImposterOnlineTileDto(Long id, String athleteName, String photoUrl, String logoUrl, boolean flipped,
                                  Boolean imposter, String flippedByName) {
        this.id = id;
        this.athleteName = athleteName;
        this.photoUrl = photoUrl;
        this.logoUrl = logoUrl;
        this.flipped = flipped;
        this.imposter = imposter;
        this.flippedByName = flippedByName;
    }

    public Long getId() { return id; }
    public String getAthleteName() { return athleteName; }
    public String getPhotoUrl() { return photoUrl; }
    public String getLogoUrl() { return logoUrl; }
    public boolean isFlipped() { return flipped; }
    public Boolean getImposter() { return imposter; }
    public String getFlippedByName() { return flippedByName; }
}
