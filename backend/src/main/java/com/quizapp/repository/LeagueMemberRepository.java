package com.quizapp.repository;

import com.quizapp.model.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, Long> {
    List<LeagueMember> findByLeague_IdOrderByJoinedAtAscIdAsc(Long leagueId);
    List<LeagueMember> findByUserId(Long userId);
    List<LeagueMember> findByUserIdAndLeftAtIsNull(Long userId);
    long countByLeague_IdAndLeftAtIsNull(Long leagueId);
    Optional<LeagueMember> findByLeague_IdAndUserId(Long leagueId, Long userId);
    boolean existsByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByLeague_Id(Long leagueId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
