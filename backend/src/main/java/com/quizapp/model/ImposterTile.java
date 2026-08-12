package com.quizapp.model;

import jakarta.persistence.*;

// One tile on an Imposter board. The subject (athlete) is always visible to
// players before it's flipped - flipping only reveals the verdict (fits vs
// imposter), not the identity, which is the opposite of how Grid works.
@Entity
@Table(name = "imposter_tiles")
public class ImposterTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imposter_grid_id", nullable = false)
    private ImposterGrid imposterGrid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(nullable = false)
    private boolean imposter = false;

    // Only meaningful when imposter=true - which genuine subject this
    // imposter took the place of (e.g. Harry Winks replacing Alan Shearer
    // in a top-10 PL scorers grid). Revealed only after every tile on the
    // board has been flipped, never during play.
    @ManyToOne
    @JoinColumn(name = "replaced_athlete_id")
    private Athlete replacedAthlete;

    // Only meaningful when the parent grid's displayMode is NAME_AND_LOGO -
    // same optional-club concept as a regular Grid entry, for the same reason
    // (an athlete has no logo of its own to show).
    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    // Which of this athlete's additional photos this tile should show,
    // instead of the athlete's primary photoUrl. Null means "use the primary
    // photo" - same concept as GridEntry.selectedPhoto.
    @ManyToOne
    @JoinColumn(name = "selected_photo_id")
    private AthletePhoto selectedPhoto;

    public AthletePhoto getSelectedPhoto() {
        return selectedPhoto;
    }

    public void setSelectedPhoto(AthletePhoto selectedPhoto) {
        this.selectedPhoto = selectedPhoto;
    }

    // Which photo to show once this tile is flipped and turns out NOT to be
    // an imposter. Null means "keep showing whatever was shown before the
    // flip" (selectedPhoto, or the athlete's primary photo). Ignored when
    // revealCorrectUseDefaultPhoto is true, which explicitly means "the
    // athlete's own primary photo" regardless of what selectedPhoto is set
    // to for the before-flip state.
    @ManyToOne
    @JoinColumn(name = "reveal_correct_photo_id")
    private AthletePhoto revealCorrectPhoto;

    @Column(nullable = false)
    private boolean revealCorrectUseDefaultPhoto = false;

    public AthletePhoto getRevealCorrectPhoto() {
        return revealCorrectPhoto;
    }

    public void setRevealCorrectPhoto(AthletePhoto revealCorrectPhoto) {
        this.revealCorrectPhoto = revealCorrectPhoto;
    }

    public boolean isRevealCorrectUseDefaultPhoto() {
        return revealCorrectUseDefaultPhoto;
    }

    public void setRevealCorrectUseDefaultPhoto(boolean revealCorrectUseDefaultPhoto) {
        this.revealCorrectUseDefaultPhoto = revealCorrectUseDefaultPhoto;
    }

    // Which photo to show once this tile is flipped and turns out to BE an
    // imposter. Same three-state behavior as revealCorrectPhoto above (keep
    // as-is / explicit default / specific photo). Only one of these two
    // reveal-photo pairs is ever actually relevant for a given tile, since a
    // tile's imposter status is fixed by the admin - which one applies
    // depends on isImposter.
    @ManyToOne
    @JoinColumn(name = "reveal_imposter_photo_id")
    private AthletePhoto revealImposterPhoto;

    @Column(nullable = false)
    private boolean revealImposterUseDefaultPhoto = false;

    public AthletePhoto getRevealImposterPhoto() {
        return revealImposterPhoto;
    }

    public void setRevealImposterPhoto(AthletePhoto revealImposterPhoto) {
        this.revealImposterPhoto = revealImposterPhoto;
    }

    public boolean isRevealImposterUseDefaultPhoto() {
        return revealImposterUseDefaultPhoto;
    }

    public void setRevealImposterUseDefaultPhoto(boolean revealImposterUseDefaultPhoto) {
        this.revealImposterUseDefaultPhoto = revealImposterUseDefaultPhoto;
    }

    @Column(nullable = false)
    private int orderIndex = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImposterGrid getImposterGrid() {
        return imposterGrid;
    }

    public void setImposterGrid(ImposterGrid imposterGrid) {
        this.imposterGrid = imposterGrid;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public boolean isImposter() {
        return imposter;
    }

    public void setImposter(boolean imposter) {
        this.imposter = imposter;
    }

    public Athlete getReplacedAthlete() {
        return replacedAthlete;
    }

    public void setReplacedAthlete(Athlete replacedAthlete) {
        this.replacedAthlete = replacedAthlete;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
