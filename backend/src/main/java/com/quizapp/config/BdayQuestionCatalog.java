package com.quizapp.config;

import java.util.List;

// Hardcoded quiz content for the one-off birthday-quiz feature (see
// BdayController/BdayQuizService) - deliberately not admin-editable content:
// the host supplies real questions (and correct answers) over chat, they get
// added here, and a redeploy picks them up. No admin authoring UI needed for
// a party that happens once.
//
// Map-pin coordinates are {x, y} fractions (0..1) of MAP_IMAGE_URL's
// *rendered* width/height, matching exactly what the frontend's
// getBoundingClientRect()-based click handler computes - see
// MapPinQuestion.vue. These placeholder coordinates are rough guesses, not
// calibrated against the real map image yet.
public final class BdayQuestionCatalog {

    private BdayQuestionCatalog() {
    }

    public static final String MAP_IMAGE_URL = "/norway-map.svg";
    public static final double MAP_TOLERANCE = 0.06;
    public static final int MAP_POINTS = 10;

    public static class MapQuestion {
        public final String id;
        public final String prompt;
        public final double correctX;
        public final double correctY;
        public final String correctCityName;

        public MapQuestion(String id, String prompt, double correctX, double correctY, String correctCityName) {
            this.id = id;
            this.prompt = prompt;
            this.correctX = correctX;
            this.correctY = correctY;
            this.correctCityName = correctCityName;
        }
    }

    public static class Tile {
        public final String id;
        public final String label;
        public final String correctOwner; // "A" or "B"

        public Tile(String id, String label, String correctOwner) {
            this.id = id;
            this.label = label;
            this.correctOwner = correctOwner;
        }
    }

    public static class TileQuestion {
        public final String id;
        public final String prompt;
        public final String personALabel;
        public final String personBLabel;
        public final List<Tile> tiles;

        public TileQuestion(String id, String prompt, String personALabel, String personBLabel, List<Tile> tiles) {
            this.id = id;
            this.prompt = prompt;
            this.personALabel = personALabel;
            this.personBLabel = personBLabel;
            this.tiles = tiles;
        }
    }

    // --- Placeholder content - replace/extend as real questions arrive ---

    public static final List<MapQuestion> MAP_QUESTIONS = List.of(
            new MapQuestion("map-1", "(Placeholder) Where was the birthday host born?", 0.23, 0.85, "Oslo")
    );

    public static final List<TileQuestion> TILE_QUESTIONS = List.of(
            new TileQuestion("tiles-1", "(Placeholder) Who does this belong to?", "Me", "My friend", List.of(
                    new Tile("tiles-1-a", "Loves coffee", "A"),
                    new Tile("tiles-1-b", "Has a cat", "B"),
                    new Tile("tiles-1-c", "Plays guitar", "A"),
                    new Tile("tiles-1-d", "Afraid of spiders", "B")
            ))
    );

    public static MapQuestion findMapQuestion(String id) {
        return MAP_QUESTIONS.stream().filter(q -> q.id.equals(id)).findFirst().orElse(null);
    }
}
