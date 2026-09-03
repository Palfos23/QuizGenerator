package com.quizapp.controller;

import com.quizapp.dto.PenaltyShootoutAdminDetailDto;
import com.quizapp.dto.PenaltyShootoutRequest;
import com.quizapp.dto.PenaltyShootoutSummaryDto;
import com.quizapp.service.PenaltyShootoutAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/penalty-shootouts")
public class AdminPenaltyShootoutController {

    private final PenaltyShootoutAdminService penaltyShootoutAdminService;

    public AdminPenaltyShootoutController(PenaltyShootoutAdminService penaltyShootoutAdminService) {
        this.penaltyShootoutAdminService = penaltyShootoutAdminService;
    }

    @GetMapping
    public List<PenaltyShootoutSummaryDto> findAll() {
        return penaltyShootoutAdminService.findAll();
    }

    @GetMapping("/{id}")
    public PenaltyShootoutAdminDetailDto getOne(@PathVariable Long id) {
        return penaltyShootoutAdminService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<PenaltyShootoutAdminDetailDto> create(@Valid @RequestBody PenaltyShootoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(penaltyShootoutAdminService.create(request));
    }

    @PutMapping("/{id}")
    public PenaltyShootoutAdminDetailDto update(@PathVariable Long id, @Valid @RequestBody PenaltyShootoutRequest request) {
        return penaltyShootoutAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        penaltyShootoutAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
