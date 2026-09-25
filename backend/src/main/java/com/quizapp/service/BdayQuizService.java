package com.quizapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.config.BdayQuestionCatalog;
import com.quizapp.dto.BdayGuestDto;
import com.quizapp.dto.BdayQuizDto;
import com.quizapp.dto.BdayResultDto;
import com.quizapp.dto.BdaySubmitRequest;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.BdayAnswer;
import com.quizapp.model.BdayGuest;
import com.quizapp.repository.BdayAnswerRepository;
import com.quizapp.repository.BdayGuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BdayQuizService {

    private final BdayGuestRepository guestRepository;
    private final BdayAnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    public BdayQuizService(BdayGuestRepository guestRepository, BdayAnswerRepository answerRepository, ObjectMapper objectMapper) {
        this.guestRepository = guestRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BdayGuestDto> listPendingGuests() {
        return guestRepository.findByStatusOrderByCreatedAtAsc(BdayGuest.Status.PENDING).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BdayGuestDto> leaderboard() {
        return guestRepository.findByStatusOrderByScoreDescCompletedAtAsc(BdayGuest.Status.DONE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BdayGuestDto addGuest(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter a name.");
        }
        if (guestRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("\"" + name + "\" is already on the guest list.");
        }
        BdayGuest guest = new BdayGuest();
        guest.setName(name);
        return toDto(guestRepository.save(guest));
    }

    @Transactional
    public void deleteGuest(Long id) {
        BdayGuest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No guest found with id " + id));
        if (guest.getStatus() != BdayGuest.Status.PENDING) {
            throw new IllegalStateException("This guest has already finished the quiz.");
        }
        guestRepository.delete(guest);
    }

    @Transactional(readOnly = true)
    public BdayQuizDto getQuiz(Long guestId) {
        requirePendingGuest(guestId);

        BdayQuizDto dto = new BdayQuizDto();
        dto.setMapImageUrl(BdayQuestionCatalog.MAP_IMAGE_URL);
        dto.setMapQuestions(BdayQuestionCatalog.MAP_QUESTIONS.stream()
                .map(q -> new BdayQuizDto.MapQuestionDto(q.id, q.prompt))
                .collect(Collectors.toList()));
        dto.setTileQuestions(BdayQuestionCatalog.TILE_QUESTIONS.stream()
                .map(q -> new BdayQuizDto.TileQuestionDto(q.id, q.prompt, q.personALabel, q.personBLabel,
                        q.tiles.stream()
                                .map(t -> new BdayQuizDto.TileDto(t.id, t.label))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public BdayResultDto submitAnswers(Long guestId, BdaySubmitRequest request) {
        BdayGuest guest = requirePendingGuest(guestId);

        int totalScore = 0;
        int totalMax = 0;
        List<BdayResultDto.MapResult> mapResults = new ArrayList<>();
        List<BdayResultDto.TileResult> tileResults = new ArrayList<>();

        List<BdaySubmitRequest.MapAnswer> mapAnswers = request.getMapAnswers() != null
                ? request.getMapAnswers() : List.of();
        for (BdayQuestionCatalog.MapQuestion q : BdayQuestionCatalog.MAP_QUESTIONS) {
            BdaySubmitRequest.MapAnswer answer = mapAnswers.stream()
                    .filter(a -> q.id.equals(a.getQuestionId()))
                    .findFirst().orElse(null);

            boolean correct = false;
            int points = 0;
            double distanceKm = -1;
            Map<String, Object> responsePayload = Map.of();
            if (answer != null) {
                double clickLat = BdayQuestionCatalog.clickToLat(answer.getY());
                double clickLng = BdayQuestionCatalog.clickToLng(answer.getX());
                distanceKm = BdayQuestionCatalog.distanceKm(clickLat, clickLng, q.correctLat, q.correctLng);
                correct = distanceKm <= q.toleranceKm;
                points = correct ? BdayQuestionCatalog.MAP_POINTS : 0;
                responsePayload = Map.of("x", answer.getX(), "y", answer.getY());
            }
            totalScore += points;
            totalMax += BdayQuestionCatalog.MAP_POINTS;
            mapResults.add(new BdayResultDto.MapResult(q.id, correct, q.correctCityName, points, distanceKm));
            saveAnswer(guest, q.id, responsePayload, points, BdayQuestionCatalog.MAP_POINTS);
        }

        List<BdaySubmitRequest.TileAnswer> tileAnswers = request.getTileAnswers() != null
                ? request.getTileAnswers() : List.of();
        Map<String, String> chosenByTileId = tileAnswers.stream()
                .collect(Collectors.toMap(BdaySubmitRequest.TileAnswer::getTileId,
                        BdaySubmitRequest.TileAnswer::getChosenOwner, (a, b) -> a));

        for (BdayQuestionCatalog.TileQuestion q : BdayQuestionCatalog.TILE_QUESTIONS) {
            int points = 0;
            Map<String, String> questionResponse = new java.util.LinkedHashMap<>();
            for (BdayQuestionCatalog.Tile tile : q.tiles) {
                String chosen = chosenByTileId.get(tile.id);
                boolean correct = tile.correctOwner.equals(chosen);
                if (correct) {
                    points++;
                }
                if (chosen != null) {
                    questionResponse.put(tile.id, chosen);
                }
                tileResults.add(new BdayResultDto.TileResult(tile.id, correct, tile.correctOwner));
            }
            totalScore += points;
            totalMax += q.tiles.size();
            saveAnswer(guest, q.id, questionResponse, points, q.tiles.size());
        }

        guest.setScore(totalScore);
        guest.setStatus(BdayGuest.Status.DONE);
        guest.setCompletedAt(Instant.now());
        guestRepository.save(guest);

        BdayResultDto result = new BdayResultDto();
        result.setScore(totalScore);
        result.setMaxScore(totalMax);
        result.setMapResults(mapResults);
        result.setTileResults(tileResults);
        return result;
    }

    private BdayGuest requirePendingGuest(Long guestId) {
        BdayGuest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("No guest found with id " + guestId));
        if (guest.getStatus() != BdayGuest.Status.PENDING) {
            throw new IllegalStateException("This guest has already finished the quiz.");
        }
        return guest;
    }

    private void saveAnswer(BdayGuest guest, String questionId, Object responsePayload, int points, int maxPoints) {
        BdayAnswer row = new BdayAnswer();
        row.setGuest(guest);
        row.setQuestionId(questionId);
        row.setResponseJson(writeJson(responsePayload));
        row.setPointsEarned(points);
        row.setMaxPoints(maxPoints);
        answerRepository.save(row);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private BdayGuestDto toDto(BdayGuest guest) {
        return new BdayGuestDto(guest.getId(), guest.getName(), guest.getStatus().name(),
                guest.getScore(), guest.getCompletedAt());
    }
}
