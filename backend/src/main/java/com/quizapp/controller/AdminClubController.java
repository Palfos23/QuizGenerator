package com.quizapp.controller;

import com.quizapp.dto.ClubDto;
import com.quizapp.model.Sport;
import com.quizapp.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/clubs")
public class AdminClubController {

    private final ClubService clubService;

    public AdminClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    public List<ClubDto> search(@RequestParam(required = false) Sport sport, @RequestParam(required = false) String name) {
        return clubService.search(sport, name);
    }

    @PostMapping
    public ResponseEntity<ClubDto> create(@Valid @RequestBody ClubDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clubService.create(dto));
    }

    @PutMapping("/{id}")
    public ClubDto update(@PathVariable Long id, @Valid @RequestBody ClubDto dto) {
        return clubService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clubService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
