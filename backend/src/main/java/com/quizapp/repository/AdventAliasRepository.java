package com.quizapp.repository;

import com.quizapp.model.AdventAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdventAliasRepository extends JpaRepository<AdventAlias, Long> {

    List<AdventAlias> findByDayIdIn(Collection<Long> dayIds);

    List<AdventAlias> findByDayId(Long dayId);

    Optional<AdventAlias> findByDayIdAndNormalizedText(Long dayId, String normalizedText);

    @Modifying
    @Transactional
    void deleteByDayIdIn(Collection<Long> dayIds);
}
