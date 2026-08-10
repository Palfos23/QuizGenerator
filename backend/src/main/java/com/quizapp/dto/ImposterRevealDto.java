package com.quizapp.dto;

public class ImposterRevealDto {
    private String imposterName;
    private String replacedName; // null if the admin didn't set one for this imposter

    public ImposterRevealDto(String imposterName, String replacedName) {
        this.imposterName = imposterName;
        this.replacedName = replacedName;
    }

    public String getImposterName() {
        return imposterName;
    }

    public String getReplacedName() {
        return replacedName;
    }
}
