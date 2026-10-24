package com.quizapp.repository;

import com.quizapp.model.BdaySetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BdaySettingRepository extends JpaRepository<BdaySetting, String> {
}
