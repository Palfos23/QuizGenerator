package com.quizapp.dto;

// One player's guessed name, in submission order, visible to everyone while
// a round is still open - so a joined-late device still sees who's answered
// what so far, same purpose as TensionAnsweredSoFarDto.
public class BullseyeOnlineAnswerDto {
    private String name;
    private String guessedName;

    public BullseyeOnlineAnswerDto(String name, String guessedName) {
        this.name = name;
        this.guessedName = guessedName;
    }

    public String getName() { return name; }
    public String getGuessedName() { return guessedName; }
}
