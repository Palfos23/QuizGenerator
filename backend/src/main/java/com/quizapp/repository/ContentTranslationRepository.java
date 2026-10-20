package com.quizapp.repository;

import com.quizapp.model.ContentTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContentTranslationRepository extends JpaRepository<ContentTranslation, Long> {

    List<ContentTranslation> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /** The ids (of the given ones) that have at least one admin-written version in that language. */
    @Query("select distinct t.entityId from ContentTranslation t where t.entityType = :type and t.lang = :lang and t.entityId in :ids")
    List<Long> findTranslatedIds(@Param("type") String type, @Param("lang") String lang, @Param("ids") Collection<Long> ids);

    List<ContentTranslation> findByEntityTypeAndEntityIdInAndLang(String entityType, Collection<Long> entityIds, String lang);
}
