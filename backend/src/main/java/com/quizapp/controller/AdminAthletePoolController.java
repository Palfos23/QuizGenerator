package com.quizapp.controller;

import com.quizapp.dto.AthletePoolDto;
import com.quizapp.dto.AthletePoolRequest;
import com.quizapp.dto.AthletePoolSummaryDto;
import com.quizapp.service.AthletePoolService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/athlete-pools")
public class AdminAthletePoolController {

    private final AthletePoolService poolService;

    public AdminAthletePoolController(AthletePoolService poolService) {
        this.poolService = poolService;
    }

    @GetMapping
    public List<AthletePoolSummaryDto> findAll() {
        return poolService.findAll();
    }

    @GetMapping("/{id}")
    public AthletePoolDto getOne(@PathVariable Long id) {
        return poolService.getOne(id);
    }

    @PostMapping
    public AthletePoolDto create(@Valid @RequestBody AthletePoolRequest request) {
        return poolService.create(request);
    }

    @PutMapping("/{id}")
    public AthletePoolDto update(@PathVariable Long id, @Valid @RequestBody AthletePoolRequest request) {
        return poolService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        poolService.delete(id);
    }
}
