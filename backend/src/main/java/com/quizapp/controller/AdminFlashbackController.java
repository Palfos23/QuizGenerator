package com.quizapp.controller;

import com.quizapp.dto.FlashbackYearDto;
import com.quizapp.dto.FlashbackYearRequest;
import com.quizapp.dto.FlashbackYearSummaryDto;
import com.quizapp.service.FlashbackAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/flashback")
public class AdminFlashbackController {

    private final FlashbackAdminService flashbackAdminService;

    public AdminFlashbackController(FlashbackAdminService flashbackAdminService) {
        this.flashbackAdminService = flashbackAdminService;
    }

    @GetMapping
    public List<FlashbackYearSummaryDto> findAll() {
        return flashbackAdminService.findAll();
    }

    @GetMapping("/{id}")
    public FlashbackYearDto getOne(@PathVariable Long id) {
        return flashbackAdminService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<FlashbackYearDto> create(@Valid @RequestBody FlashbackYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flashbackAdminService.create(request));
    }

    @PutMapping("/{id}")
    public FlashbackYearDto update(@PathVariable Long id, @Valid @RequestBody FlashbackYearRequest request) {
        return flashbackAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flashbackAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
