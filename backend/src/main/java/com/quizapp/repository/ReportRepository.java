package com.quizapp.repository;

import com.quizapp.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReportedBy_EmailOrderByCreatedAtDesc(String email);
    List<Report> findAllByOrderByCreatedAtDesc();
}
