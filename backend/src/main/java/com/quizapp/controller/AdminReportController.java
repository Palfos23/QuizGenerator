package com.quizapp.controller;

import com.quizapp.dto.ReportDto;
import com.quizapp.dto.ResolveReportRequest;
import com.quizapp.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportDto> findAll() {
        return reportService.listAll();
    }

    @PostMapping("/{id}/resolve")
    public ReportDto resolve(@PathVariable Long id, @RequestBody ResolveReportRequest request) {
        return reportService.resolve(id, request);
    }
}
