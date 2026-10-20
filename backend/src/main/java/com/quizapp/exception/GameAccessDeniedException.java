package com.quizapp.exception;

public class GameAccessDeniedException extends RuntimeException {
    public GameAccessDeniedException(String gameLabel) {
        super("Du har ikke tilgang til " + gameLabel + " akkurat nå - kontakt en administrator.");
    }
}
