package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class TensionCategoryDto {

    private Long id;

    @NotBlank
    private String name;

    private List<String> options;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
