package com.quizapp.model;

import jakarta.persistence.*;

// A small key/value setting for the birthday night (whether Mario Kart times can be entered, the points per rank, ...).
// The column names avoid "key" and "value" - both are reserved words in some databases.
@Entity
@Table(name = "bday_settings")
public class BdaySetting {

    @Id
    @Column(name = "setting_key", length = 50)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;

    public BdaySetting() {}

    public BdaySetting(String key, String settingValue) {
        this.key = key;
        this.settingValue = settingValue;
    }

    public String getKey() { return key; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
}
