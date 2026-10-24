package com.quizapp.service;

import com.quizapp.dto.BdayNightDto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The rest of the birthday night, next to the quiz: Mario Kart best laps (guests type their own time once, the host can
 * fix or remove it), the music quiz teams (the host keeps each team's score, which counts for every member), and the
 * wheel (one spin per guest - the host records the bonus points and can have the app pick whose turn it is).
 * The scoring that ties it together is in {@link BdayStandingsService}.
 */
@Service
public class BdayNightService {

    private static final Pattern LAP_TIME = Pattern.compile("^(?:(\\d{1,2}):)?(\\d{1,3})(?:\\.(\\d{1,3}))?$");
    static final int MIN_LAP_MS = 5_000;
    static final int MAX_LAP_MS = 10 * 60 * 1000;

    private final BdayGuestRepository guestRepository;
    private final BdayMarioKartTimeRepository timeRepository;
    private final BdayTeamRepository teamRepository;
    private final BdayWheelSpinRepository spinRepository;
    private final BdayStandingsService standingsService;
    private final SecureRandom random = new SecureRandom();

    public BdayNightService(BdayGuestRepository guestRepository, BdayMarioKartTimeRepository timeRepository,
                            BdayTeamRepository teamRepository, BdayWheelSpinRepository spinRepository,
                            BdayStandingsService standingsService) {
        this.guestRepository = guestRepository;
        this.timeRepository = timeRepository;
        this.teamRepository = teamRepository;
        this.spinRepository = spinRepository;
        this.standingsService = standingsService;
    }

    // ================================================================== Mario Kart

    /** "1:23.456", "1:23,45", "83.456" and "1:23" are all fine. Returns milliseconds; anything else is an error. */
    static int parseLapTime(String raw) {
        String s = raw == null ? "" : raw.trim().replace(',', '.');
        Matcher m = LAP_TIME.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException("Skriv tiden som m:ss.mmm - f.eks. 1:23.456");
        }
        int minutes = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        int seconds = Integer.parseInt(m.group(2));
        if (m.group(1) != null && seconds >= 60) {
            throw new IllegalArgumentException("Sekundene må være under 60 når du skriver minutter - f.eks. 1:23.456");
        }
        String fraction = m.group(3) == null ? "" : m.group(3);
        int millis = fraction.isEmpty() ? 0 : Integer.parseInt((fraction + "00").substring(0, 3));
        long total = ((long) minutes * 60 + seconds) * 1000 + millis;
        if (total < MIN_LAP_MS || total > MAX_LAP_MS) {
            throw new IllegalArgumentException("Tiden ser ikke riktig ut - en runde tar mellom 5 sekunder og 10 minutter.");
        }
        return (int) total;
    }

    /** What the guests' own Mario Kart page shows: everyone, whether they have a time yet, and the ranking so far. */
    @Transactional(readOnly = true)
    public MarioKartPublic marioKartPublic() {
        Set<Long> withTime = timeRepository.findAll().stream().map(t -> t.getGuest().getId()).collect(Collectors.toSet());
        List<GuestSlot> guests = guestRepository.findAll().stream()
                .sorted(Comparator.comparing(g -> g.getName().toLowerCase(Locale.ROOT)))
                .map(g -> new GuestSlot(g.getId(), g.getName(), withTime.contains(g.getId())))
                .collect(Collectors.toList());
        return new MarioKartPublic(standingsService.settings().marioKartOpen(), guests, standingsService.lapRanking());
    }

    /** A guest enters their own best lap - once, and only while the host has registration open. */
    @Transactional
    public LapResult enterOwnTime(Long guestId, String rawTime) {
        if (!standingsService.settings().marioKartOpen()) {
            throw new IllegalStateException("Registreringen av Mario Kart-tider er ikke åpen akkurat nå.");
        }
        BdayGuest guest = requireGuest(guestId);
        int ms = parseLapTime(rawTime);
        if (timeRepository.findByGuest_Id(guestId).isPresent()) {
            throw new IllegalStateException("Det er allerede registrert en tid for " + guest.getName() + " - si fra til arrangøren hvis den er feil.");
        }
        BdayMarioKartTime t = new BdayMarioKartTime();
        t.setGuest(guest);
        t.setTimeMs(ms);
        timeRepository.save(t);
        return resultFor(guestId);
    }

    @Transactional(readOnly = true)
    public MarioKartAdmin marioKartAdmin() {
        Map<Long, LapEntry> laps = standingsService.lapRanking().stream().collect(Collectors.toMap(LapEntry::guestId, l -> l));
        List<GuestLap> guests = guestRepository.findAll().stream()
                .map(g -> {
                    LapEntry l = laps.get(g.getId());
                    return new GuestLap(g.getId(), g.getName(), l == null ? null : l.timeMs(), l == null ? null : l.rank(), l == null ? 0 : l.points());
                })
                .sorted(Comparator.comparing((GuestLap g) -> g.timeMs() == null)
                        .thenComparing(g -> g.timeMs() == null ? 0 : g.timeMs())
                        .thenComparing(g -> g.name().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        return new MarioKartAdmin(standingsService.settings(), guests);
    }

    /** The host sets (or replaces) a guest's time. */
    @Transactional
    public LapResult setTime(Long guestId, String rawTime) {
        BdayGuest guest = requireGuest(guestId);
        int ms = parseLapTime(rawTime);
        BdayMarioKartTime t = timeRepository.findByGuest_Id(guestId).orElseGet(() -> {
            BdayMarioKartTime fresh = new BdayMarioKartTime();
            fresh.setGuest(guest);
            return fresh;
        });
        t.setTimeMs(ms);
        timeRepository.save(t);
        return resultFor(guestId);
    }

    @Transactional
    public void clearTime(Long guestId) {
        requireGuest(guestId);
        timeRepository.deleteByGuest_Id(guestId);
    }

    private LapResult resultFor(Long guestId) {
        LapEntry l = standingsService.lapRanking().stream().filter(e -> e.guestId().equals(guestId)).findFirst().orElseThrow();
        return new LapResult(l.timeMs(), l.rank(), l.points());
    }

    // ================================================================== teams (music quiz)

    @Transactional(readOnly = true)
    public TeamsAdmin teamsAdmin() {
        List<BdayGuest> guests = guestRepository.findAll();
        List<Team> teams = teamRepository.findAllByOrderByNameAsc().stream()
                .map(t -> new Team(t.getId(), t.getName(), t.getMusicScore(), guests.stream()
                        .filter(g -> g.getTeam() != null && g.getTeam().getId().equals(t.getId()))
                        .sorted(Comparator.comparing(g -> g.getName().toLowerCase(Locale.ROOT)))
                        .map(g -> new Person(g.getId(), g.getName()))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
        List<Person> unassigned = guests.stream()
                .filter(g -> g.getTeam() == null)
                .sorted(Comparator.comparing(g -> g.getName().toLowerCase(Locale.ROOT)))
                .map(g -> new Person(g.getId(), g.getName()))
                .collect(Collectors.toList());
        return new TeamsAdmin(teams, unassigned);
    }

    @Transactional
    public TeamsAdmin createTeam(TeamRequest request) {
        String name = request.name().trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Skriv inn et lagnavn.");
        if (teamRepository.existsByNameIgnoreCase(name)) throw new IllegalArgumentException("Det finnes allerede et lag som heter \"" + name + "\".");
        BdayTeam team = new BdayTeam();
        team.setName(name);
        team.setMusicScore(checkedScore(request.musicScore() == null ? 0 : request.musicScore()));
        teamRepository.save(team);
        return teamsAdmin();
    }

    @Transactional
    public TeamsAdmin updateTeam(Long id, TeamRequest request) {
        BdayTeam team = requireTeam(id);
        String name = request.name().trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Skriv inn et lagnavn.");
        if (!name.equalsIgnoreCase(team.getName()) && teamRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Det finnes allerede et lag som heter \"" + name + "\".");
        }
        team.setName(name);
        if (request.musicScore() != null) team.setMusicScore(checkedScore(request.musicScore()));
        teamRepository.save(team);
        return teamsAdmin();
    }

    /** Deleting a team takes its members off it (they stay in the game) - their music points go with it. */
    @Transactional
    public TeamsAdmin deleteTeam(Long id) {
        BdayTeam team = requireTeam(id);
        for (BdayGuest g : guestRepository.findAll()) {
            if (g.getTeam() != null && g.getTeam().getId().equals(id)) {
                g.setTeam(null);
                guestRepository.save(g);
            }
        }
        teamRepository.delete(team);
        return teamsAdmin();
    }

    /** Puts a guest on a team, or takes them off (teamId null). */
    @Transactional
    public TeamsAdmin assignTeam(Long guestId, Long teamId) {
        BdayGuest guest = requireGuest(guestId);
        guest.setTeam(teamId == null ? null : requireTeam(teamId));
        guestRepository.save(guest);
        return teamsAdmin();
    }

    private static int checkedScore(int score) {
        if (score < -1000 || score > 1000) throw new IllegalArgumentException("Poengsummen må være mellom -1000 og 1000.");
        return score;
    }

    // ================================================================== the wheel

    @Transactional(readOnly = true)
    public WheelAdmin wheelAdmin() {
        Map<Long, BdayWheelSpin> spins = spinRepository.findAll().stream().collect(Collectors.toMap(s -> s.getGuest().getId(), s -> s));
        List<Person> candidates = guestRepository.findAll().stream()
                .filter(g -> !spins.containsKey(g.getId()))
                .sorted(Comparator.comparing(g -> g.getName().toLowerCase(Locale.ROOT)))
                .map(g -> new Person(g.getId(), g.getName()))
                .collect(Collectors.toList());
        List<Spin> done = spins.values().stream()
                .sorted(Comparator.comparing(BdayWheelSpin::getSpunAt).reversed())
                .map(s -> new Spin(s.getGuest().getId(), s.getGuest().getName(), s.getBonusPoints(), s.getNote(), s.getSpunAt()))
                .collect(Collectors.toList());
        return new WheelAdmin(candidates, done);
    }

    /** Whose turn is it? A random guest who hasn't spun yet. Nothing is recorded until the host saves the spin. */
    @Transactional(readOnly = true)
    public Person pickNextSpinner() {
        List<Person> candidates = wheelAdmin().candidates();
        if (candidates.isEmpty()) throw new IllegalStateException("Alle har allerede spunnet hjulet.");
        return candidates.get(random.nextInt(candidates.size()));
    }

    @Transactional
    public WheelAdmin recordSpin(SpinRequest request) {
        if (request.guestId() == null) throw new IllegalArgumentException("Velg hvem som spant.");
        BdayGuest guest = requireGuest(request.guestId());
        if (spinRepository.findByGuest_Id(guest.getId()).isPresent()) {
            throw new IllegalStateException(guest.getName() + " har allerede spunnet hjulet.");
        }
        BdayWheelSpin spin = new BdayWheelSpin();
        spin.setGuest(guest);
        apply(spin, request);
        spinRepository.save(spin);
        return wheelAdmin();
    }

    @Transactional
    public WheelAdmin updateSpin(Long guestId, SpinRequest request) {
        BdayWheelSpin spin = spinRepository.findByGuest_Id(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke noe hjulspinn for denne gjesten."));
        apply(spin, request);
        spinRepository.save(spin);
        return wheelAdmin();
    }

    /** Removing a spin gives the guest their turn back. */
    @Transactional
    public WheelAdmin deleteSpin(Long guestId) {
        spinRepository.deleteByGuest_Id(guestId);
        return wheelAdmin();
    }

    private void apply(BdayWheelSpin spin, SpinRequest request) {
        if (request.points() < -100 || request.points() > 100) {
            throw new IllegalArgumentException("Bonuspoengene må være mellom -100 og 100.");
        }
        spin.setBonusPoints(request.points());
        String note = request.note() == null ? "" : request.note().trim();
        spin.setNote(note.isEmpty() ? null : note);
    }

    // ================================================================== helpers

    private BdayGuest requireGuest(Long id) {
        return guestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fant ikke gjest med id " + id));
    }

    private BdayTeam requireTeam(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fant ikke lag med id " + id));
    }
}
