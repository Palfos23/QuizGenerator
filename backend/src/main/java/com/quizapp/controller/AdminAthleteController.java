package com.quizapp.controller;

import com.quizapp.dto.AthleteDto;
import com.quizapp.model.Sport;
import com.quizapp.service.AthleteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/athletes")
public class AdminAthleteController {

    private final AthleteService athleteService;

    public AdminAthleteController(AthleteService athleteService) {
        this.athleteService = athleteService;
    }

    @GetMapping
    public List<AthleteDto> search(
            @RequestParam(required = false) Sport sport,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String name) {
        return athleteService.search(sport, team, name);
    }

    @PostMapping
    public ResponseEntity<AthleteDto> create(@Valid @RequestBody AthleteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(athleteService.create(dto));
    }

    @PutMapping("/{id}")
    public AthleteDto update(@PathVariable Long id, @Valid @RequestBody AthleteDto dto) {
        return athleteService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        athleteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
