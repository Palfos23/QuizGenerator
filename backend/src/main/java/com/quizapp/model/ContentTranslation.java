package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * An admin-written version of one text field of a game board (or question), in one language. The board's own
 * columns hold the base text; this table only holds the optional Norwegian ("no") / English ("en") versions that
 * replace it when a player has that language selected. Keyed loosely (type + id) rather than by foreign key so
 * one table serves every game - deleting a board just leaves harmless orphan rows.
 */
@Entity
@Table(name = "content_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"entity_type", "entity_id", "field_name", "lang"}),
        indexes = @Index(columnList = "entity_type, entity_id"))
public class ContentTranslation {

    public static final String TYPE_GRID = "GRID";
    public static final String TYPE_LINEUP = "LINEUP";
    public static final String TYPE_IMPOSTER = "IMPOSTER";
    public static final String TYPE_BULLSEYE = "BULLSEYE";
    public static final String TYPE_TENSION = "TENSION";
    public static final String TYPE_FIVEOHONE = "FIVEOHONE";
    public static final String TYPE_FLASHBACK = "FLASHBACK";
    public static final String TYPE_SHOOTOUT = "SHOOTOUT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // "title", "theme", ... or "hints#0" for one element of a list
    @Column(name = "field_name", nullable = false, length = 40)
    private String fieldName;

    // "no" or "en"
    @Column(nullable = false, length = 5)
    private String lang;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public ContentTranslation() {}

    public ContentTranslation(String entityType, Long entityId, String fieldName, String lang, String content) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.fieldName = fieldName;
        this.lang = lang;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getFieldName() { return fieldName; }
    public String getLang() { return lang; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; this.updatedAt = Instant.now(); }
}
