package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** The shapes the birthday night's other competitions (Mario Kart, music quiz teams, the wheel, the combined board) send and receive. */
public final class BdayNightDto {

    private BdayNightDto() {}

    // ---------------------------------------------------------------- the combined leaderboard

    /** One guest's total and where it comes from. */
    public record PlayerStanding(Long guestId, String name, String teamName, int rank, boolean quizDone, int quizScore,
                                 Integer marioKartTimeMs, Integer marioKartRank, int marioKartPoints,
                                 int musicPoints, int bonusPoints, int total) {}

    public record TeamStanding(Long id, String name, int musicScore, int members) {}

    /** One line of the Mario Kart best-lap ranking. */
    public record LapEntry(int rank, Long guestId, String name, int timeMs, int points) {}

    public record Standings(List<PlayerStanding> players, List<TeamStanding> teams, List<LapEntry> marioKart, boolean marioKartOpen) {}

    // ---------------------------------------------------------------- Mario Kart

    public record Person(Long id, String name) {}

    /** What the guests' own Mario Kart page needs: who can still enter a time, and the ranking so far. */
    public record MarioKartPublic(boolean open, List<GuestSlot> guests, List<LapEntry> ranking) {}

    public record GuestSlot(Long id, String name, boolean hasTime) {}

    public record TimeRequest(@NotBlank @Size(max = 20) String time) {}

    public record LapResult(int timeMs, int rank, int points) {}

    /** The settings that decide how lap times turn into points, and whether guests may enter them. */
    public record Settings(boolean marioKartOpen, String marioKartPoints, int marioKartOtherPoints) {}

    public record GuestLap(Long guestId, String name, Integer timeMs, Integer rank, int points) {}

    public record MarioKartAdmin(Settings settings, List<GuestLap> guests) {}

    // ---------------------------------------------------------------- teams (music quiz)

    public record TeamRequest(@NotBlank @Size(max = 40) String name, Integer musicScore) {}

    public record AssignTeamRequest(Long teamId) {}

    public record Team(Long id, String name, int musicScore, List<Person> members) {}

    public record TeamsAdmin(List<Team> teams, List<Person> unassigned) {}

    // ---------------------------------------------------------------- the wheel

    public record Spin(Long guestId, String name, int bonusPoints, String note, Instant spunAt) {}

    public record WheelAdmin(List<Person> candidates, List<Spin> spins) {}

    public record SpinRequest(Long guestId, int points, @Size(max = 100) String note) {}
}
