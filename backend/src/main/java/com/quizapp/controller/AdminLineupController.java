package com.quizapp.controller;

import com.quizapp.dto.LineupAdminDetailDto;
import com.quizapp.dto.LineupRequest;
import com.quizapp.dto.LineupSummaryDto;
import com.quizapp.service.LineupAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/lineups")
public class AdminLineupController {

    private final LineupAdminService lineupAdminService;

    public AdminLineupController(LineupAdminService lineupAdminService) {
        this.lineupAdminService = lineupAdminService;
    }

    @GetMapping
    public List<LineupSummaryDto> findAll() {
        return lineupAdminService.findAll();
    }

    @GetMapping("/{id}")
    public LineupAdminDetailDto getOne(@PathVariable Long id) {
        return lineupAdminService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<LineupAdminDetailDto> create(@Valid @RequestBody LineupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lineupAdminService.create(request));
    }

    @PutMapping("/{id}")
    public LineupAdminDetailDto update(@PathVariable Long id, @Valid @RequestBody LineupRequest request) {
        return lineupAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lineupAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
