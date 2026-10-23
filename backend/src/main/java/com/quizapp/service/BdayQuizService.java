package com.quizapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.config.BdayQuestionCatalog;
import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.BdayAnswer;
import com.quizapp.model.BdayGuest;
import com.quizapp.model.BdayQuestion;
import com.quizapp.repository.BdayAnswerRepository;
import com.quizapp.repository.BdayGuestRepository;
import com.quizapp.repository.BdayQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The one-off birthday quiz. Guests pick their name on their own phone (no login) and answer: one map question,
 * then the host's statements (Pål / Erik / Begge / Ingen), photo questions (Pål / Erik) and text questions. Every
 * question except the map is one point; the map question is worth {@link BdayQuestionCatalog#MAP_POINTS}. Scoring
 * is always recomputed here - nothing about correctness is ever sent to, or trusted from, the guest's phone.
 * Everything the host does (players, questions, score fixes) lives in the admin controller.
 */
@Service
public class BdayQuizService {

    private static final List<BdayQuestion.Type> TYPE_ORDER =
            List.of(BdayQuestion.Type.STATEMENT, BdayQuestion.Type.PHOTO, BdayQuestion.Type.TEXT);

    private final BdayGuestRepository guestRepository;
    private final BdayAnswerRepository answerRepository;
    private final BdayQuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public BdayQuizService(BdayGuestRepository guestRepository, BdayAnswerRepository answerRepository,
                           BdayQuestionRepository questionRepository, ObjectMapper objectMapper) {
        this.guestRepository = guestRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ guests: what the players see

    /** The names still waiting to play - alphabetical, so a guest finds theirs quickly. */
    @Transactional(readOnly = true)
    public List<BdayGuestDto> listPendingGuests() {
        return guestRepository.findByStatusOrderByCreatedAtAsc(BdayGuest.Status.PENDING).stream()
                .sorted(Comparator.comparing(g -> g.getName().toLowerCase(Locale.ROOT)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BdayGuestDto> leaderboard() {
        return guestRepository.findByStatusOrderByScoreDescCompletedAtAsc(BdayGuest.Status.DONE).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ guests: admin

    /** Everyone, waiting first (alphabetical), then the finished ones by score. */
    @Transactional(readOnly = true)
    public List<BdayGuestDto> listAllGuests() {
        List<BdayGuestDto> all = new ArrayList<>(listPendingGuests());
        all.addAll(leaderboard());
        return all;
    }

    @Transactional
    public BdayGuestDto addGuest(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Skriv inn et navn.");
        }
        if (guestRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("\"" + name + "\" står allerede på gjestelisten.");
        }
        BdayGuest guest = new BdayGuest();
        guest.setName(name);
        return toDto(guestRepository.save(guest));
    }

    @Transactional
    public void deleteGuest(Long id) {
        BdayGuest guest = requireGuest(id);
        if (guest.getStatus() != BdayGuest.Status.PENDING) {
            throw new IllegalStateException("Denne gjesten har allerede fullført quizen.");
        }
        guestRepository.delete(guest);
    }

    /** Host override: set a finished guest's total score by hand (e.g. to fix a text answer that should have counted). */
    @Transactional
    public BdayGuestDto setScore(Long id, int score) {
        BdayGuest guest = requireGuest(id);
        if (guest.getStatus() != BdayGuest.Status.DONE) {
            throw new IllegalStateException("Bare en gjest som er ferdig kan få endret poengsummen.");
        }
        int max = maxTotalScore();
        if (score < 0 || score > max) {
            throw new IllegalArgumentException("Poengsummen må være mellom 0 og " + max + ".");
        }
        guest.setScore(score);
        return toDto(guestRepository.save(guest));
    }

    /**
     * Host override: puts a finished guest back on the waiting list - their answers and score are thrown away
     * and they can pick their name and play again. Already-waiting guests are left as they are.
     */
    @Transactional
    public BdayGuestDto resetGuest(Long id) {
        BdayGuest guest = requireGuest(id);
        if (guest.getStatus() == BdayGuest.Status.DONE) {
            answerRepository.deleteByGuest_Id(guest.getId());
            guest.setStatus(BdayGuest.Status.PENDING);
            guest.setScore(0);
            guest.setCompletedAt(null);
            guest = guestRepository.save(guest);
        }
        return toDto(guest);
    }

    /** A finished guest's answer sheet - what they answered and what each answer scored. */
    @Transactional(readOnly = true)
    public List<BdayAnswerViewDto> guestAnswers(Long id) {
        requireGuest(id);
        Map<String, BdayQuestion> questions = questionRepository.findAll().stream()
                .collect(Collectors.toMap(q -> questionKey(q), q -> q));
        List<BdayAnswerViewDto> out = new ArrayList<>();
        for (BdayAnswer a : answerRepository.findByGuest_IdOrderByIdAsc(id)) {
            JsonNode json = readJson(a.getResponseJson());
            if (a.getQuestionId().startsWith("map-")) {
                BdayQuestionCatalog.MapQuestion mq = BdayQuestionCatalog.MAP_QUESTIONS.stream()
                        .filter(q -> q.id.equals(a.getQuestionId())).findFirst().orElse(null);
                String where = json.has("x") ? "Pinne satt på kartet" : "(ikke besvart)";
                out.add(new BdayAnswerViewDto(mq != null ? mq.prompt : a.getQuestionId(), "MAP", where,
                        a.getPointsEarned(), a.getMaxPoints()));
                continue;
            }
            BdayQuestion q = questions.get(a.getQuestionId());
            String value = json.path("value").asText("");
            String shown = q != null && q.getType() != BdayQuestion.Type.TEXT && !value.isEmpty()
                    ? BdayQuestionCatalog.optionLabel(value) : value;
            out.add(new BdayAnswerViewDto(q != null ? q.getPrompt() : "(slettet spørsmål)",
                    q != null ? q.getType().name() : "TEXT", shown.isEmpty() ? "(tomt)" : shown,
                    a.getPointsEarned(), a.getMaxPoints()));
        }
        return out;
    }

    // ------------------------------------------------------------------ questions: admin

    @Transactional(readOnly = true)
    public List<BdayQuestionAdminDto> listQuestions() {
        return orderedQuestions().stream().map(this::toAdminDto).collect(Collectors.toList());
    }

    @Transactional
    public BdayQuestionAdminDto createQuestion(BdayQuestionRequest request) {
        BdayQuestion q = new BdayQuestion();
        apply(q, request);
        q.setSortOrder(questionRepository.findAll().stream().mapToInt(BdayQuestion::getSortOrder).max().orElse(0) + 1);
        return toAdminDto(questionRepository.save(q));
    }

    @Transactional
    public BdayQuestionAdminDto updateQuestion(Long id, BdayQuestionRequest request) {
        BdayQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke spørsmål med id " + id));
        apply(q, request);
        return toAdminDto(questionRepository.save(q));
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fant ikke spørsmål med id " + id);
        }
        questionRepository.deleteById(id);
    }

    /** What the question can score at most for a player who gets everything right: the map plus a point per question. */
    @Transactional(readOnly = true)
    public int maxTotalScore() {
        return BdayQuestionCatalog.MAP_QUESTIONS.size() * BdayQuestionCatalog.MAP_POINTS + (int) questionRepository.count();
    }

    private void apply(BdayQuestion q, BdayQuestionRequest request) {
        BdayQuestion.Type type;
        try {
            type = BdayQuestion.Type.valueOf(request.getType() == null ? "" : request.getType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent spørsmålstype.");
        }
        String prompt = request.getPrompt() == null ? "" : request.getPrompt().trim();
        q.setType(type);
        switch (type) {
            case STATEMENT -> {
                if (prompt.isEmpty()) throw new IllegalArgumentException("Skriv inn påstanden.");
                q.setPrompt(prompt);
                q.setCorrectOption(requireOption(request.getCorrectOption(), BdayQuestionCatalog.STATEMENT_OPTIONS));
                q.setPhotoUrl(null);
                q.setAcceptedAnswers(null);
            }
            case PHOTO -> {
                String url = request.getPhotoUrl() == null ? "" : request.getPhotoUrl().trim();
                if (url.isEmpty()) throw new IllegalArgumentException("Legg til et bilde.");
                if (!(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/"))) {
                    throw new IllegalArgumentException("Bildet må være en lenke (http/https) eller et opplastet bilde.");
                }
                q.setPrompt(prompt.isEmpty() ? "Hvem er på bildet?" : prompt);
                q.setPhotoUrl(url);
                q.setCorrectOption(requireOption(request.getCorrectOption(), BdayQuestionCatalog.PHOTO_OPTIONS));
                q.setAcceptedAnswers(null);
            }
            case TEXT -> {
                if (prompt.isEmpty()) throw new IllegalArgumentException("Skriv inn spørsmålet.");
                List<String> accepted = parseAccepted(request.getAcceptedAnswers());
                if (accepted.isEmpty()) throw new IllegalArgumentException("Legg til minst ett godkjent svar.");
                q.setPrompt(prompt);
                q.setAcceptedAnswers(String.join("\n", accepted));
                q.setPhotoUrl(null);
                q.setCorrectOption(null);
            }
        }
    }

    private static String requireOption(String raw, List<String> allowed) {
        String option = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(option)) {
            throw new IllegalArgumentException("Velg riktig svar.");
        }
        return option;
    }

    // ------------------------------------------------------------------ playing

    @Transactional(readOnly = true)
    public BdayQuizDto getQuiz(Long guestId) {
        requirePendingGuest(guestId);

        BdayQuizDto dto = new BdayQuizDto();
        dto.setMapImageUrl(BdayQuestionCatalog.MAP_IMAGE_URL);
        dto.setMapQuestions(BdayQuestionCatalog.MAP_QUESTIONS.stream()
                .map(q -> new BdayQuizDto.MapQuestionDto(q.id, q.prompt))
                .collect(Collectors.toList()));
        dto.setQuestions(orderedQuestions().stream()
                .map(q -> new BdayQuizDto.QuestionDto(questionKey(q), q.getType().name(), q.getPrompt(),
                        q.getType() == BdayQuestion.Type.PHOTO ? q.getPhotoUrl() : null))
                .collect(Collectors.toList()));
        dto.setStatementOptions(options(BdayQuestionCatalog.STATEMENT_OPTIONS));
        dto.setPhotoOptions(options(BdayQuestionCatalog.PHOTO_OPTIONS));
        return dto;
    }

    @Transactional
    public BdayResultDto submitAnswers(Long guestId, BdaySubmitRequest request) {
        BdayGuest guest = requirePendingGuest(guestId);

        int totalScore = 0;
        int totalMax = 0;
        List<BdayResultDto.MapResult> mapResults = new ArrayList<>();

        List<BdaySubmitRequest.MapAnswer> mapAnswers = request.getMapAnswers() != null ? request.getMapAnswers() : List.of();
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

        Map<String, String> valueByQuestionId = new HashMap<>();
        if (request.getAnswers() != null) {
            for (BdaySubmitRequest.Answer a : request.getAnswers()) {
                if (a.getQuestionId() != null) valueByQuestionId.putIfAbsent(a.getQuestionId(), a.getValue() == null ? "" : a.getValue());
            }
        }

        Map<BdayQuestion.Type, int[]> perSection = new EnumMap<>(BdayQuestion.Type.class); // {correct, total}
        for (BdayQuestion q : orderedQuestions()) {
            String value = valueByQuestionId.getOrDefault(questionKey(q), "");
            boolean correct = isCorrect(q, value);
            int points = correct ? 1 : 0;
            totalScore += points;
            totalMax += 1;
            int[] counts = perSection.computeIfAbsent(q.getType(), t -> new int[2]);
            counts[0] += points;
            counts[1] += 1;
            saveAnswer(guest, questionKey(q), Map.of("value", value), points, 1);
        }

        guest.setScore(totalScore);
        guest.setStatus(BdayGuest.Status.DONE);
        guest.setCompletedAt(Instant.now());
        guestRepository.save(guest);

        BdayResultDto result = new BdayResultDto();
        result.setScore(totalScore);
        result.setMaxScore(totalMax);
        result.setMapResults(mapResults);
        result.setSections(TYPE_ORDER.stream()
                .filter(perSection::containsKey)
                .map(t -> new BdayResultDto.SectionResult(t.name(), perSection.get(t)[0], perSection.get(t)[1]))
                .collect(Collectors.toList()));
        return result;
    }

    /** Is this answer right for this question? Text answers ignore case, accents and punctuation. */
    static boolean isCorrect(BdayQuestion q, String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) return false;
        if (q.getType() == BdayQuestion.Type.TEXT) {
            String given = normalizeAnswer(value);
            return !given.isEmpty() && parseAccepted(q.getAcceptedAnswers()).stream()
                    .anyMatch(accepted -> normalizeAnswer(accepted).equals(given));
        }
        return value.equalsIgnoreCase(q.getCorrectOption());
    }

    /** "  Førde!! " and "forde" and "FØRDE" are the same answer. */
    static String normalizeAnswer(String s) {
        // ø and æ don't decompose into a base letter plus an accent, so they're mapped by hand (å does, via NFD)
        String lower = s.toLowerCase(Locale.ROOT).replace("ø", "o").replace("æ", "ae");
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return decomposed.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    static List<String> parseAccepted(String raw) {
        if (raw == null) return List.of();
        return Arrays.stream(raw.split("[\\n|]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ helpers

    /** Statements, then photo questions, then text questions - each in the order the host wrote them. */
    private List<BdayQuestion> orderedQuestions() {
        return questionRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .sorted(Comparator.comparingInt(q -> TYPE_ORDER.indexOf(q.getType())))
                .collect(Collectors.toList());
    }

    private static String questionKey(BdayQuestion q) {
        return "q-" + q.getId();
    }

    private static List<BdayQuizDto.OptionDto> options(List<String> codes) {
        return codes.stream()
                .map(c -> new BdayQuizDto.OptionDto(c, BdayQuestionCatalog.optionLabel(c)))
                .collect(Collectors.toList());
    }

    private BdayGuest requireGuest(Long id) {
        return guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke gjest med id " + id));
    }

    private BdayGuest requirePendingGuest(Long guestId) {
        BdayGuest guest = requireGuest(guestId);
        if (guest.getStatus() != BdayGuest.Status.PENDING) {
            throw new IllegalStateException("Denne gjesten har allerede fullført quizen.");
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

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private BdayQuestionAdminDto toAdminDto(BdayQuestion q) {
        return new BdayQuestionAdminDto(q.getId(), q.getType().name(), q.getPrompt(), q.getPhotoUrl(),
                q.getCorrectOption(), q.getAcceptedAnswers());
    }

    private BdayGuestDto toDto(BdayGuest guest) {
        return new BdayGuestDto(guest.getId(), guest.getName(), guest.getStatus().name(),
                guest.getScore(), guest.getCompletedAt());
    }
}
