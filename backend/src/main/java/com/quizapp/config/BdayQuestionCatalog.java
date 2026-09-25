package com.quizapp.config;

import java.util.List;

// Hardcoded quiz content for the one-off birthday-quiz feature (see
// BdayController/BdayQuizService) - deliberately not admin-editable content:
// the host supplies real questions (and correct answers) over chat, they get
// added here, and a redeploy picks them up. No admin authoring UI needed for
// a party that happens once.
//
// The frontend still just reports a click as {x, y} fractions (0..1) of
// MAP_IMAGE_URL's rendered width/height (see MapPinQuestion.vue) - it knows
// nothing about geography. The backend converts that click into a real
// lat/lng using the map's calibration bounds below, then scores it against
// each question's real correctLat/correctLng with a great-circle (Haversine)
// distance in km. MAP_IMAGE_URL is the actual NordNordWest "Norway location
// map.svg" from Wikimedia Commons (CC BY-SA 3.0 / GFDL, blank base map, no
// text/labels) - MAP_TOP_LAT/BOTTOM_LAT/LEFT_LNG/RIGHT_LNG are the exact
// bounds Wikipedia's own Module:Location_map/data/Norway uses to place
// markers on this same file, so linear interpolation against them lines up
// with the image's drawn coastline (the image itself is a deliberately
// N/S-stretched equirectangular projection - these bounds already account
// for that; do not "correct" them against a plain globe).
public final class BdayQuestionCatalog {

    private BdayQuestionCatalog() {
    }

    public static final String MAP_IMAGE_URL = "/norway-map.svg";
    public static final double MAP_TOP_LAT = 71.5;
    public static final double MAP_BOTTOM_LAT = 57.6;
    public static final double MAP_LEFT_LNG = 4.1;
    public static final double MAP_RIGHT_LNG = 31.6;
    public static final int MAP_POINTS = 10;
    private static final double EARTH_RADIUS_KM = 6371.0;

    public static class MapQuestion {
        public final String id;
        public final String prompt;
        public final double correctLat;
        public final double correctLng;
        public final double toleranceKm;
        public final String correctCityName;

        public MapQuestion(String id, String prompt, double correctLat, double correctLng, double toleranceKm, String correctCityName) {
            this.id = id;
            this.prompt = prompt;
            this.correctLat = correctLat;
            this.correctLng = correctLng;
            this.toleranceKm = toleranceKm;
            this.correctCityName = correctCityName;
        }
    }

    // x/y are the frontend's raw click fractions (0..1) of the map image.
    public static double clickToLat(double y) {
        return MAP_TOP_LAT - y * (MAP_TOP_LAT - MAP_BOTTOM_LAT);
    }

    public static double clickToLng(double x) {
        return MAP_LEFT_LNG + x * (MAP_RIGHT_LNG - MAP_LEFT_LNG);
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
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
            new MapQuestion("map-1", "Where are Pål and Erik born?", 61.4522, 5.8572, 20, "Førde")
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
