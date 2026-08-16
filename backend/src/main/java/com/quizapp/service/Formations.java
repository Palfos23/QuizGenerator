package com.quizapp.service;

import java.util.LinkedHashMap;
import java.util.Map;

// The fixed set of formation shapes admin can choose for a Starting XI board.
// Each value is the row sizes from goalkeeper up to attack, always summing to
// 11 - slotIndex 0 is the keeper, then rows fill left-to-right in ascending
// order. Mirrored on the frontend in services/formations.js so both the admin
// slot-assignment UI and the pitch renderer agree on layout without a round
// trip - keep the two in sync if this list ever changes.
public final class Formations {

    private static final Map<String, int[]> SHAPES = new LinkedHashMap<>();
    static {
        SHAPES.put("4-4-2", new int[]{1, 4, 4, 2});
        SHAPES.put("4-3-3", new int[]{1, 4, 3, 3});
        SHAPES.put("4-2-3-1", new int[]{1, 4, 2, 3, 1});
        SHAPES.put("4-1-4-1", new int[]{1, 4, 1, 4, 1});
        SHAPES.put("3-5-2", new int[]{1, 3, 5, 2});
        SHAPES.put("3-4-3", new int[]{1, 3, 4, 3});
        SHAPES.put("5-3-2", new int[]{1, 5, 3, 2});
        SHAPES.put("4-5-1", new int[]{1, 4, 5, 1});
    }

    private Formations() {
    }

    public static boolean isKnown(String formation) {
        return formation != null && SHAPES.containsKey(formation);
    }

    public static int slotCount(String formation) {
        int[] rows = SHAPES.get(formation);
        if (rows == null) return 0;
        int total = 0;
        for (int r : rows) total += r;
        return total;
    }
}
