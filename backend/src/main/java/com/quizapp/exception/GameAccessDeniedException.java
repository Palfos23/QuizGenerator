package com.quizapp.exception;

public class GameAccessDeniedException extends RuntimeException {
    public GameAccessDeniedException(String gameLabel) {
        super("You don't currently have access to " + gameLabel + " - contact an administrator.");
    }
}
