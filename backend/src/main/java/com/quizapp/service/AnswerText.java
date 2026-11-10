package com.quizapp.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * How free-text answers are compared (the daily quiz and the advent calendar share this): "  Ålesund!! ", "alesund" and
 * "ÅLESUND" are the same answer. ø and æ don't decompose into a letter plus an accent, so they are mapped by hand.
 */
public final class AnswerText {

    private AnswerText() {}

    public static String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT).replace("ø", "o").replace("æ", "ae");
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return decomposed.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    /**
     * What identifies "the same answer" when grouping and remembering decisions: the normalised text, or - for an answer
     * with no letters or digits at all ("???") - the lower-cased text itself, so even that can be grouped.
     */
    public static String key(String s) {
        String n = normalize(s);
        return n.isEmpty() ? (s == null ? "" : s.trim().toLowerCase(Locale.ROOT)) : n;
    }

    /**
     * Looks like a misspelling of the correct answer (one letter wrong, missing, extra or swapped - two for long answers),
     * rather than a different answer. Only a hint for the admin: nothing is graded from it. Both texts are normalised first.
     */
    public static boolean likelyTypo(String given, String correct) {
        String a = normalize(given);
        String b = normalize(correct);
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) return false;
        int longest = Math.max(a.length(), b.length());
        if (longest < 5) return false; // short answers differ by one letter too easily ("Nils" / "Nina")
        int allowed = longest >= 10 ? 2 : 1;
        return distance(a, b) <= allowed;
    }

    /** Edit distance where swapping two neighbouring letters counts as one change (the commonest typo). */
    static int distance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }
}
