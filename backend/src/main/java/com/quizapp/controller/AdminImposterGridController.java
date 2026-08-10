package com.quizapp.controller;

import com.quizapp.dto.ImposterGridAdminDetailDto;
import com.quizapp.dto.ImposterGridRequest;
import com.quizapp.dto.ImposterGridSummaryDto;
import com.quizapp.service.ImposterGridAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/imposter-grids")
public class AdminImposterGridController {

    private final ImposterGridAdminService adminService;

    public AdminImposterGridController(ImposterGridAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<ImposterGridSummaryDto> findAll() {
        return adminService.findAll();
    }

    @GetMapping("/{id}")
    public ImposterGridAdminDetailDto getOne(@PathVariable Long id) {
        return adminService.getOne(id);
    }

    @PostMapping
    public ImposterGridAdminDetailDto create(@Valid @RequestBody ImposterGridRequest request) {
        return adminService.create(request);
    }

    @PutMapping("/{id}")
    public ImposterGridAdminDetailDto update(@PathVariable Long id, @Valid @RequestBody ImposterGridRequest request) {
        return adminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminService.delete(id);
    }
}
