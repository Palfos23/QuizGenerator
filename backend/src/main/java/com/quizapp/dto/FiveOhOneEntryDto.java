package com.quizapp.dto;

public class FiveOhOneEntryDto {
    private Long id;
    private String name;
    // Integer, not int - null means "pool subject with no explicit value",
    // filled in by FiveOhOneCategoryService's entire-category-pool expansion.
    // A saved, explicit entry (the only shape a create/update request sends)
    // always carries a real value.
    private Integer value;
    // Which Subject this entry is (or, for a pool-only row, always is) -
    // null only for a legacy free-text entry that predates this link.
    private Long athleteId;

    public FiveOhOneEntryDto() {
    }

    public FiveOhOneEntryDto(Long id, String name, Integer value) {
        this(id, name, value, null);
    }

    public FiveOhOneEntryDto(Long id, String name, Integer value, Long athleteId) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.athleteId = athleteId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
    public Long getAthleteId() { return athleteId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
}
