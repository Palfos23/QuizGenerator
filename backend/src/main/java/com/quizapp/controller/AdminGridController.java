package com.quizapp.controller;

import com.quizapp.dto.GridAdminDetailDto;
import com.quizapp.dto.GridRequest;
import com.quizapp.dto.GridSummaryDto;
import com.quizapp.service.GridAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/grids")
public class AdminGridController {

    private final GridAdminService gridAdminService;

    public AdminGridController(GridAdminService gridAdminService) {
        this.gridAdminService = gridAdminService;
    }

    @GetMapping
    public List<GridSummaryDto> findAll() {
        return gridAdminService.findAll();
    }

    @GetMapping("/{id}")
    public GridAdminDetailDto getOne(@PathVariable Long id) {
        return gridAdminService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<GridAdminDetailDto> create(@Valid @RequestBody GridRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gridAdminService.create(request));
    }

    @PutMapping("/{id}")
    public GridAdminDetailDto update(@PathVariable Long id, @Valid @RequestBody GridRequest request) {
        return gridAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gridAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
