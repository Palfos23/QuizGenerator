package com.quizapp.controller;

import com.quizapp.dto.ReportDto;
import com.quizapp.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportDto> submit(@Valid @RequestBody ReportDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.submit(authentication.getName(), dto));
    }

    @GetMapping("/mine")
    public List<ReportDto> mine(Authentication authentication) {
        return reportService.listMine(authentication.getName());
    }
}
