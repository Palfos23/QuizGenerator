package com.quizapp.repository;

import com.quizapp.model.AdventProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdventProfileRepository extends JpaRepository<AdventProfile, Long> {
}
