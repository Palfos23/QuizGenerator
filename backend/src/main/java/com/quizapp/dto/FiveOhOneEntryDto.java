package com.quizapp.dto;

public class FiveOhOneEntryDto {
    private Long id;
    private String name;
    private int value;

    public FiveOhOneEntryDto() {
    }

    public FiveOhOneEntryDto(Long id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
