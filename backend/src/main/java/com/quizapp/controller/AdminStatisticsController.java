package com.quizapp.controller;

import com.quizapp.dto.AdminStatisticsDto;
import com.quizapp.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only dashboard figures for the admin Statistics page. ADMIN-role JWT
 * required - enforced in SecurityConfig via the /api/admin/** matcher.
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    public AdminStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public AdminStatisticsDto get() {
        return statisticsService.build();
    }
}
