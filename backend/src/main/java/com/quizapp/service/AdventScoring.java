package com.quizapp.service;

import com.quizapp.model.AdventDay;
import com.quizapp.model.AdventEntry;
import com.quizapp.model.AdventType;

import java.text.Normalizer;
import java.util.*;

/**
 * The rules of the advent calendar, as plain functions. A player writes four answers; the game master has hidden
 * answers; every player answer that matches a (different) hidden answer is a "match", and the kind of door decides what
 * the matches are worth. Nothing here touches the database - AdventService feeds it the day, the entries and the game
 * master's corrections.
 */
public final class AdventScoring {

    private AdventScoring() {}

    public static final int FINALE_DAY = 24;
    public static final int FINALE_MULTIPLIER = 3;
    public static final int JOKERS_PER_SEASON = 3;
    public static final int WILDCARD_BONUS = 3;
    public static final int GOLD_ANSWER_POINTS = 5;
    public static final int OPPOSITE_POINTS = 5;
    public static final int RISK_MISS_POINTS = -1;

    /** "  Ålesund!! " and "alesund" and "ÅLESUND" are the same answer; ø and æ don't decompose, so they're mapped by hand. */
    public static String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT).replace("ø", "o").replace("æ", "ae");
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return decomposed.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public static boolean isFinale(int dayNumber) {
        return dayNumber == FINALE_DAY;
    }

    /**
     * Which hidden answer a player answer counts as: its index, or -1 for none. The game master's corrections
     * (normalised text -> index or -1) win over the automatic reading, which is an exact match after normalising.
     */
    public static int matchIndex(String answer, List<String> hidden, Map<String, Integer> corrections) {
        String n = normalize(answer);
        if (n.isEmpty()) return -1;
        Integer corrected = corrections.get(n);
        if (corrected != null) return corrected < hidden.size() ? corrected : -1;
        for (int i = 0; i < hidden.size(); i++) {
            if (normalize(hidden.get(i)).equals(n)) return i;
        }
        return -1;
    }

    /** What one entry earned: its points before the joker and after it. */
    public record EntryResult(AdventEntry entry, int[] answerMatches, int matches, boolean wildcardHit, int basePoints, int points) {
        public int jokerBonus() {
            return points - basePoints;
        }
    }

    /** The points a door's rules give for a number of matches (the finale's tripling included, the joker not). */
    public static int basePoints(AdventType type, int dayNumber, boolean risky, int matches, boolean wildcardHit) {
        int points = switch (type) {
            case NORMAL -> matches;
            case DOUBLE -> 2 * matches;
            case RISK_REWARD -> !risky ? matches : matches == 0 ? RISK_MISS_POINTS : 2 * matches;
            case WILDCARD -> matches + (wildcardHit ? WILDCARD_BONUS : 0);
            case OPPOSITE -> matches == 0 ? OPPOSITE_POINTS : 0;
            case ONE_OF_FOUR -> matches >= 1 ? GOLD_ANSWER_POINTS : 0;
        };
        return isFinale(dayNumber) ? points * FINALE_MULTIPLIER : points;
    }

    /** The joker doubles what you earned - but a minus is left alone, so it can never make a bad day worse. */
    public static int withJoker(int basePoints, boolean jokerUsed) {
        return jokerUsed && basePoints > 0 ? basePoints * 2 : basePoints;
    }

    /** Everything about one closed door: each entry's result, how many players hit each hidden answer, the game master's points. */
    public static final class DayResult {
        public final AdventDay day;
        public final Map<Long, EntryResult> byUser = new LinkedHashMap<>();
        public final int entrants;
        /** Per hidden answer: how many players matched it. */
        public final int[] matchedPlayers;
        public final int gamemasterPoints;

        private DayResult(AdventDay day, List<AdventEntry> entries, Map<String, Integer> corrections) {
            this.day = day;
            List<String> hidden = day.getAnswers();
            this.matchedPlayers = new int[hidden.size()];
            boolean anyMatch = false;
            for (AdventEntry entry : entries) {
                List<String> answers = entry.getAnswers();
                int[] perAnswer = new int[answers.size()];
                Set<Integer> distinct = new TreeSet<>();
                for (int i = 0; i < answers.size(); i++) {
                    perAnswer[i] = matchIndex(answers.get(i), hidden, corrections);
                    if (perAnswer[i] >= 0) distinct.add(perAnswer[i]);
                }
                for (int idx : distinct) matchedPlayers[idx]++;
                if (!distinct.isEmpty()) anyMatch = true;
                boolean wildcardHit = day.getType() == AdventType.WILDCARD && day.getWildcardIndex() != null
                        && distinct.contains(day.getWildcardIndex());
                int base = basePoints(day.getType(), day.getDayNumber(), entry.isRisky(), distinct.size(), wildcardHit);
                int points = isFinale(day.getDayNumber()) ? base : withJoker(base, entry.isJokerUsed());
                byUser.put(entry.getUserId(), new EntryResult(entry, perAnswer, distinct.size(), wildcardHit, base, points));
            }
            this.entrants = entries.size();
            this.gamemasterPoints = gamemasterPoints(day.getType(), day.getDayNumber(), entrants, anyMatch, matchedPlayers);
        }

        public static DayResult compute(AdventDay day, List<AdventEntry> entries, Map<String, Integer> corrections) {
            return new DayResult(day, entries, corrections);
        }
    }

    /**
     * The game master scores when nobody hits a single hidden answer: 1 point (2 on an X2 door). On the upside-down door
     * they score for every one of their answers that somebody guessed instead - up to 4. The finale triples it.
     */
    public static int gamemasterPoints(AdventType type, int dayNumber, int entrants, boolean anyMatch, int[] matchedPlayers) {
        if (entrants == 0) return 0;
        int points;
        if (type == AdventType.OPPOSITE) {
            points = (int) Arrays.stream(matchedPlayers).filter(c -> c > 0).count();
        } else if (anyMatch) {
            points = 0;
        } else {
            points = type == AdventType.DOUBLE ? 2 : 1;
        }
        return isFinale(dayNumber) ? points * FINALE_MULTIPLIER : points;
    }
}
