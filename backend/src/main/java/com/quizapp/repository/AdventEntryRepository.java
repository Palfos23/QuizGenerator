package com.quizapp.repository;

import com.quizapp.model.AdventEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdventEntryRepository extends JpaRepository<AdventEntry, Long> {

    List<AdventEntry> findByDayIdIn(Collection<Long> dayIds);

    List<AdventEntry> findByDayId(Long dayId);

    Optional<AdventEntry> findByDayIdAndUserId(Long dayId, Long userId);

    long countByDayId(Long dayId);

    List<AdventEntry> findByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByDayIdIn(Collection<Long> dayIds);
}
