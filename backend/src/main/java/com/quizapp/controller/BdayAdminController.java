package com.quizapp.controller;

import com.quizapp.dto.*;
import com.quizapp.service.BdayNightService;
import com.quizapp.service.BdayQuizService;
import com.quizapp.service.BdayStandingsService;
import com.quizapp.service.ImageStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// The host's side of the birthday quiz: players, the questions, and fixing a score by hand. Gated by the quiz's own PIN
// (BdayPinAuthFilter), not by the site's admin role - some admins are guests at the party, and this side shows the answers.
@RestController
@RequestMapping("/api/bday/admin")
public class BdayAdminController {

    private final BdayQuizService quizService;
    private final BdayNightService nightService;
    private final BdayStandingsService standingsService;
    private final ImageStorageService imageStorageService;

    public BdayAdminController(BdayQuizService quizService, BdayNightService nightService,
                               BdayStandingsService standingsService, ImageStorageService imageStorageService) {
        this.quizService = quizService;
        this.nightService = nightService;
        this.standingsService = standingsService;
        this.imageStorageService = imageStorageService;
    }

    /** A photo for a picture question - the same storage the site's admin uploads use, but behind the quiz's PIN. */
    @PostMapping("/images")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of("url", imageStorageService.upload(file.getBytes()));
    }

    @GetMapping("/guests")
    public List<BdayGuestDto> guests() {
        return quizService.listAllGuests();
    }

    @PostMapping("/guests")
    public ResponseEntity<BdayGuestDto> addGuest(@Valid @RequestBody BdayAddGuestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.addGuest(request.getName()));
    }

    @DeleteMapping("/guests/{id}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id) {
        quizService.deleteGuest(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/guests/{id}/score")
    public BdayGuestDto setScore(@PathVariable Long id, @Valid @RequestBody BdayScoreRequest request) {
        return quizService.setScore(id, request.getScore());
    }

    @PostMapping("/guests/{id}/reset")
    public BdayGuestDto resetGuest(@PathVariable Long id) {
        return quizService.resetGuest(id);
    }

    @GetMapping("/guests/{id}/answers")
    public List<BdayAnswerViewDto> answers(@PathVariable Long id) {
        return quizService.guestAnswers(id);
    }

    /** Host override: mark one of a finished guest's answers right or wrong. Body: {"correct": true}. */
    @PutMapping("/guests/{id}/answers/{answerId}")
    public List<BdayAnswerViewDto> markAnswer(@PathVariable Long id, @PathVariable Long answerId,
                                              @RequestBody java.util.Map<String, Boolean> body) {
        if (body == null || body.get("correct") == null) {
            throw new IllegalArgumentException("Mangler «correct».");
        }
        return quizService.markAnswer(id, answerId, body.get("correct"));
    }

    // the full board and the final reveal of the public one
    @GetMapping("/standings")
    public BdayNightDto.Standings standings() {
        return standingsService.standings();
    }

    @GetMapping("/reveal")
    public BdayNightDto.RevealState reveal() {
        return standingsService.revealState();
    }

    @PutMapping("/reveal")
    public BdayNightDto.RevealState updateReveal(@RequestBody BdayNightDto.RevealRequest request) {
        return standingsService.updateReveal(request);
    }

    @PostMapping("/reveal/next")
    public BdayNightDto.RevealState revealNext() {
        return standingsService.revealNext();
    }

    @GetMapping("/questions")
    public List<BdayQuestionAdminDto> questions() {
        return quizService.listQuestions();
    }

    @PostMapping("/questions")
    public ResponseEntity<BdayQuestionAdminDto> createQuestion(@Valid @RequestBody BdayQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.createQuestion(request));
    }

    @PutMapping("/questions/{id}")
    public BdayQuestionAdminDto updateQuestion(@PathVariable Long id, @Valid @RequestBody BdayQuestionRequest request) {
        return quizService.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- Mario Kart

    @GetMapping("/settings")
    public BdayNightDto.Settings settings() {
        return standingsService.settings();
    }

    @PutMapping("/settings")
    public BdayNightDto.Settings updateSettings(@RequestBody BdayNightDto.Settings settings) {
        return standingsService.updateSettings(settings);
    }

    @GetMapping("/mariokart")
    public BdayNightDto.MarioKartAdmin marioKart() {
        return nightService.marioKartAdmin();
    }

    @PutMapping("/mariokart/{guestId}")
    public BdayNightDto.LapResult setTime(@PathVariable Long guestId, @Valid @RequestBody BdayNightDto.TimeRequest request) {
        return nightService.setTime(guestId, request.time());
    }

    @DeleteMapping("/mariokart/{guestId}")
    public ResponseEntity<Void> clearTime(@PathVariable Long guestId) {
        nightService.clearTime(guestId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- music quiz teams

    @GetMapping("/teams")
    public BdayNightDto.TeamsAdmin teams() {
        return nightService.teamsAdmin();
    }

    @PostMapping("/teams")
    public BdayNightDto.TeamsAdmin createTeam(@Valid @RequestBody BdayNightDto.TeamRequest request) {
        return nightService.createTeam(request);
    }

    @PutMapping("/teams/{id}")
    public BdayNightDto.TeamsAdmin updateTeam(@PathVariable Long id, @Valid @RequestBody BdayNightDto.TeamRequest request) {
        return nightService.updateTeam(id, request);
    }

    @DeleteMapping("/teams/{id}")
    public BdayNightDto.TeamsAdmin deleteTeam(@PathVariable Long id) {
        return nightService.deleteTeam(id);
    }

    @PutMapping("/guests/{id}/team")
    public BdayNightDto.TeamsAdmin assignTeam(@PathVariable Long id, @RequestBody BdayNightDto.AssignTeamRequest request) {
        return nightService.assignTeam(id, request.teamId());
    }

    // ---------------------------------------------------------------- the wheel

    @GetMapping("/wheel")
    public BdayNightDto.WheelAdmin wheel() {
        return nightService.wheelAdmin();
    }

    @PostMapping("/wheel/pick")
    public BdayNightDto.Person pickSpinner() {
        return nightService.pickNextSpinner();
    }

    @PostMapping("/wheel/spins")
    public BdayNightDto.WheelAdmin recordSpin(@Valid @RequestBody BdayNightDto.SpinRequest request) {
        return nightService.recordSpin(request);
    }

    @PutMapping("/wheel/spins/{guestId}")
    public BdayNightDto.WheelAdmin updateSpin(@PathVariable Long guestId, @Valid @RequestBody BdayNightDto.SpinRequest request) {
        return nightService.updateSpin(guestId, request);
    }

    @DeleteMapping("/wheel/spins/{guestId}")
    public BdayNightDto.WheelAdmin deleteSpin(@PathVariable Long guestId) {
        return nightService.deleteSpin(guestId);
    }
}
