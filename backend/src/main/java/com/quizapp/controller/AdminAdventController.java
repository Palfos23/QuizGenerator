package com.quizapp.controller;

import com.quizapp.dto.AdventDto.*;
import com.quizapp.service.AdventAdminService;
import org.springframework.web.bind.annotation.*;

/** The game master's side of the advent calendar. /api/admin/** is already admin-only in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/advent")
public class AdminAdventController {

    private final AdventAdminService adminService;

    public AdminAdventController(AdventAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public AdminSeasonDto season() {
        return adminService.season();
    }

    @GetMapping("/days/{day}")
    public AdminDayDto day(@PathVariable int day) {
        return adminService.day(day);
    }

    @PutMapping("/days/{day}")
    public AdminDayDto save(@PathVariable int day, @RequestBody AdminDayRequest request) {
        return adminService.saveDay(day, request);
    }

    @GetMapping("/days/{day}/review")
    public ReviewDto review(@PathVariable int day) {
        return adminService.review(day);
    }

    @PutMapping("/days/{day}/decision")
    public ReviewDto decide(@PathVariable int day, @RequestBody DecisionRequest request) {
        return adminService.decide(day, request);
    }

    @PostMapping("/days/{day}/approve")
    public ReviewDto approve(@PathVariable int day, @RequestBody ApproveRequest request) {
        return adminService.approve(day, request.approved());
    }
}
