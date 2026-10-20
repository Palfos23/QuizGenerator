package com.quizapp.dto;

import com.quizapp.config.Translated;
import com.quizapp.model.ContentTranslation;

@Translated(type = ContentTranslation.TYPE_FIVEOHONE, fields = {"title"})
public class FiveOhOneRoomCategoryDto {

    private Long id;
    private String title;

    public FiveOhOneRoomCategoryDto(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
