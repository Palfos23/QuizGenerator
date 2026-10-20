package com.quizapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * What the host's health check calls. It deliberately does no database work: the check runs every few
 * seconds, forever, and it used to be pointed at /api/quiz/categories - a real query over the question
 * bank each time, which on its own kept the database busy around the clock.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
