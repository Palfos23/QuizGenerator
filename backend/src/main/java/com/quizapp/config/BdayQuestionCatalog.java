package com.quizapp.config;

import java.util.List;

// The fixed parts of the one-off birthday quiz (see BdayController/BdayQuizService): the map question, the two
// people the quiz is about, and the answer options. Everything else - the statements, the photo questions and the
// text questions - is written by the host on the admin page and lives in the bday_questions table.
//
// The frontend reports a map click as {x, y} fractions (0..1) of the map image's width/height (see
// MapPinQuestion.vue) and knows nothing about geography. The backend converts that click into a real lat/lng using
// the map's calibration bounds below, then scores it against the question's real correctLat/correctLng with a
// great-circle (Haversine) distance in km.
//
// MAP_IMAGE_URL is Wikimedia Commons' "Norway Vestland rel location map" (relief base map of Vestland county, no
// labels). The bounds are the ones Wikipedia's own Module:Location_map/data/Norway_Vestland uses to place markers
// on that same file, so linear interpolation against them lines up with the drawn coastline.
public final class BdayQuestionCatalog {

    private BdayQuestionCatalog() {
    }

    public static final String MAP_IMAGE_URL = "/vestland-map.jpg";
    public static final double MAP_TOP_LAT = 62.25;
    public static final double MAP_BOTTOM_LAT = 59.45;
    public static final double MAP_LEFT_LNG = 4.15;
    public static final double MAP_RIGHT_LNG = 8.9;
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

    /** The inverse of the two above - where on the map image a real place is. Used by the tests and the admin preview. */
    public static double latToY(double lat) {
        return (MAP_TOP_LAT - lat) / (MAP_TOP_LAT - MAP_BOTTOM_LAT);
    }

    public static double lngToX(double lng) {
        return (lng - MAP_LEFT_LNG) / (MAP_RIGHT_LNG - MAP_LEFT_LNG);
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

    // --- The two people the quiz is about, and what a guest can answer. ---

    public static final String PAL = "PAL";
    public static final String ERIK = "ERIK";
    public static final String BEGGE = "BEGGE";
    public static final String INGEN = "INGEN";

    /** Statement questions: who does it fit? */
    public static final List<String> STATEMENT_OPTIONS = List.of(PAL, ERIK, BEGGE, INGEN);
    /** Photo questions: who is on the photo? */
    public static final List<String> PHOTO_OPTIONS = List.of(PAL, ERIK);

    public static String optionLabel(String code) {
        return switch (code) {
            case PAL -> "Pål";
            case ERIK -> "Erik";
            case BEGGE -> "Begge";
            case INGEN -> "Ingen";
            default -> code;
        };
    }

    // The one map question: where were Pål and Erik born? Førde, 20 km either way.
    public static final List<MapQuestion> MAP_QUESTIONS = List.of(
            new MapQuestion("map-1", "Hvor er Pål og Erik født?", 61.4522, 5.8572, 20, "Førde")
    );
}
