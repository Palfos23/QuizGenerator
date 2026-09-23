package com.quizapp.controller;

import com.quizapp.dto.AthleteDto;
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
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String name) {
        return athleteService.search(sport, team, name);
    }

    @PostMapping
    public ResponseEntity<AthleteDto> create(@Valid @RequestBody AthleteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(athleteService.create(dto));
    }

    // Used by Bullseye/501's "add these as subjects" prompt for names a bulk/CSV
    // import didn't find - see AthleteService#createBulk.
    @PostMapping("/bulk")
    public ResponseEntity<List<AthleteDto>> createBulk(@Valid @RequestBody List<@Valid AthleteDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(athleteService.createBulk(dtos));
    }

    @PutMapping("/{id}")
    public AthleteDto update(@PathVariable Long id, @Valid @RequestBody AthleteDto dto) {
        return athleteService.update(id, dto);
    }

    // Data-quality scan for the "Duplicate subjects" Insights page - see
    // AthleteService#findDuplicateGroups for the actual detection logic.
    @GetMapping("/duplicates")
    public List<com.quizapp.dto.AthleteDuplicateGroupDto> duplicates(
            @RequestParam(required = false) String sport,
            @RequestParam(defaultValue = "0") int maxDistance) {
        return athleteService.findDuplicateGroups(sport, maxDistance);
    }

    @GetMapping("/{id}/grid-usage")
    public List<com.quizapp.dto.AthleteGridUsageDto> gridUsage(@PathVariable Long id) {
        return athleteService.findGridUsage(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @RequestParam(defaultValue = "false") boolean removeFromGrids) {
        athleteService.delete(id, removeFromGrids);
        return ResponseEntity.noContent().build();
    }
}
