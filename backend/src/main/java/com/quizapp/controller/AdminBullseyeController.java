package com.quizapp.controller;

import com.quizapp.dto.BullseyeQuestionAdminDetailDto;
import com.quizapp.dto.BullseyeQuestionRequest;
import com.quizapp.dto.BullseyeQuestionSummaryDto;
import com.quizapp.service.BullseyeAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bullseye")
public class AdminBullseyeController {

    private final BullseyeAdminService bullseyeAdminService;

    public AdminBullseyeController(BullseyeAdminService bullseyeAdminService) {
        this.bullseyeAdminService = bullseyeAdminService;
    }

    @GetMapping
    public List<BullseyeQuestionSummaryDto> findAll() {
        return bullseyeAdminService.findAll();
    }

    @GetMapping("/{id}")
    public BullseyeQuestionAdminDetailDto getOne(@PathVariable Long id) {
        return bullseyeAdminService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<BullseyeQuestionAdminDetailDto> create(@Valid @RequestBody BullseyeQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bullseyeAdminService.create(request));
    }

    @PutMapping("/{id}")
    public BullseyeQuestionAdminDetailDto update(@PathVariable Long id, @Valid @RequestBody BullseyeQuestionRequest request) {
        return bullseyeAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bullseyeAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
