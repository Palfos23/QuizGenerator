package com.quizapp.repository;

import com.quizapp.model.ContentTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ContentTranslationRepository extends JpaRepository<ContentTranslation, Long> {

    List<ContentTranslation> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<ContentTranslation> findByEntityTypeAndEntityIdInAndLang(String entityType, Collection<Long> entityIds, String lang);
}
