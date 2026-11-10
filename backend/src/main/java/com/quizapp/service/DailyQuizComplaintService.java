package com.quizapp.service;

import com.quizapp.dto.DailyQuizPlayStateDto;
import com.quizapp.dto.DailyQuizReviewDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.DailyQuizAnswer;
import com.quizapp.model.DailyQuizAnswerVerdict;
import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizAttemptStatus;
import com.quizapp.model.DailyQuizComplaint;
import com.quizapp.model.DailyQuizDecisionSource;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * "Be om ny vurdering": a player who thinks one of their wrong answers should have counted can ask for another look, once
 * their quiz is graded. A few per quiz, one per answer, and only while the quiz still exists (7 days). The admin sees the
 * queue, accepts (the answer becomes correct, the score and the weekly standings follow - and the answer can be accepted
 * for everyone who wrote it, now and next time) or turns it down with a short note the player can read.
 */
@Service
public class DailyQuizComplaintService {

    static final int MAX_COMPLAINTS_PER_ATTEMPT = 3;
    static final int MAX_COMMENT_LENGTH = 300;
    static final int MAX_NOTE_LENGTH = 300;

    private final DailyQuizComplaintRepository complaintRepository;
    private final DailyQuizAnswerRepository answerRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final DailyQuizService dailyQuizService;
    private final DailyQuizReviewService reviewService;
    private final AdminNotificationService adminNotificationService;

    public DailyQuizComplaintService(DailyQuizComplaintRepository complaintRepository, DailyQuizAnswerRepository answerRepository,
                                     DailyQuizAttemptRepository attemptRepository, DailyQuizService dailyQuizService,
                                     DailyQuizReviewService reviewService, AdminNotificationService adminNotificationService) {
        this.complaintRepository = complaintRepository;
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.dailyQuizService = dailyQuizService;
        this.reviewService = reviewService;
        this.adminNotificationService = adminNotificationService;
    }

    /** What can be complained about: a wrong answer to an ordinary question. Year questions are plain arithmetic, a blank has nothing to judge. */
    static boolean isComplainable(DailyQuizAnswer answer) {
        return answer.getVerdict() == DailyQuizAnswerVerdict.INCORRECT
                && answer.getAnswerText() != null && !answer.getAnswerText().isBlank()
                && !DailyQuizScoring.isScoredAsYear(answer.getQuestion());
    }

    // ------------------------------------------------------------------ player

    @Transactional
    public DailyQuizPlayStateDto complain(Long setId, Long answerId, String userEmail, String rawComment) {
        DailyQuizAnswer answer = answerRepository.findById(answerId)
                .filter(a -> a.getAttempt().getUser().getEmail().equals(userEmail))
                .filter(a -> a.getAttempt().getSet().getId().equals(setId))
                .filter(a -> !DailyQuizService.isExpired(a.getAttempt().getSet().getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke svaret."));
        DailyQuizAttempt attempt = answer.getAttempt();
        if (attempt.getStatus() != DailyQuizAttemptStatus.GRADED) {
            throw new IllegalStateException("Du kan be om ny vurdering når quizen din er ferdig rettet.");
        }
        if (answer.getVerdict() == DailyQuizAnswerVerdict.CORRECT) {
            throw new IllegalStateException("Dette svaret er allerede regnet som riktig.");
        }
        if (!isComplainable(answer)) {
            throw new IllegalStateException("Dette svaret kan du ikke be om ny vurdering av.");
        }
        if (complaintRepository.findByAnswer_Id(answerId).isPresent()) {
            throw new IllegalStateException("Du har allerede bedt om ny vurdering av dette svaret.");
        }
        if (complaintRepository.countByAnswer_Attempt_Id(attempt.getId()) >= MAX_COMPLAINTS_PER_ATTEMPT) {
            throw new IllegalStateException("Du har brukt opp de " + MAX_COMPLAINTS_PER_ATTEMPT + " klagene du har på denne quizen.");
        }
        String comment = rawComment == null ? "" : rawComment.strip();
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Kommentaren kan ikke være lengre enn " + MAX_COMMENT_LENGTH + " tegn.");
        }
        DailyQuizComplaint complaint = new DailyQuizComplaint();
        complaint.setAnswer(answer);
        complaint.setComment(comment.isEmpty() ? null : comment);
        complaintRepository.save(complaint);
        adminNotificationService.notifyAdmin("Ny klage på dagens quiz",
                attempt.getUser().getName() + " ber om ny vurdering av et svar.", "/admin/daily-quiz-answers?tab=complaints");
        return dailyQuizService.getPlayState(setId, userEmail);
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public List<DailyQuizReviewDto.Complaint> listOpen() {
        return complaintRepository.findByStatusOrderByCreatedAtAsc(DailyQuizComplaint.Status.OPEN).stream()
                .filter(c -> !DailyQuizService.isExpired(c.getAnswer().getAttempt().getSet().getQuizDate()))
                .map(c -> {
                    DailyQuizAnswer a = c.getAnswer();
                    return new DailyQuizReviewDto.Complaint(c.getId(), c.getCreatedAt(), a.getAttempt().getUser().getName(),
                            a.getAttempt().getSet().getQuizDate(), a.getQuestion().getId(), a.getQuestion().getQuestionText(),
                            a.getQuestion().getAnswer(), a.getAnswerText(), c.getComment(),
                            a.getDecidedBy() == null ? null : a.getDecidedBy().name());
                })
                .collect(Collectors.toList());
    }

    private DailyQuizComplaint requireOpen(Long id) {
        DailyQuizComplaint c = complaintRepository.findById(id)
                .filter(found -> !DailyQuizService.isExpired(found.getAnswer().getAttempt().getSet().getQuizDate()))
                .orElseThrow(() -> new ResourceNotFoundException("Fant ikke klagen."));
        if (c.getStatus() != DailyQuizComplaint.Status.OPEN) {
            throw new IllegalStateException("Denne klagen er allerede avgjort.");
        }
        return c;
    }

    /** The answer counts. rememberForEveryone also accepts it for everyone else who wrote it, and for next time. Returns answers changed. */
    @Transactional
    public int accept(Long id, boolean rememberForEveryone) {
        DailyQuizComplaint complaint = requireOpen(id);
        DailyQuizAnswer answer = complaint.getAnswer();
        int changed;
        if (rememberForEveryone) {
            changed = reviewService.decideGroup(answer.getQuestion().getId(), answer.getAnswerText(), true, true);
        } else {
            boolean did = reviewService.setVerdict(answer, true, DailyQuizDecisionSource.COMPLAINT);
            if (did) reviewService.afterDecision(answer.getAttempt());
            changed = did ? 1 : 0;
        }
        finish(complaint, DailyQuizComplaint.Status.ACCEPTED, null);
        return changed;
    }

    @Transactional
    public void reject(Long id, String rawNote) {
        DailyQuizComplaint complaint = requireOpen(id);
        String note = rawNote == null ? "" : rawNote.strip();
        if (note.length() > MAX_NOTE_LENGTH) throw new IllegalArgumentException("Notatet kan ikke være lengre enn " + MAX_NOTE_LENGTH + " tegn.");
        finish(complaint, DailyQuizComplaint.Status.REJECTED, note.isEmpty() ? null : note);
    }

    // the outcome is something the player should look at, whichever way it went
    private void finish(DailyQuizComplaint complaint, DailyQuizComplaint.Status status, String note) {
        DailyQuizComplaint fresh = complaintRepository.findById(complaint.getId()).orElse(complaint);
        fresh.setStatus(status);
        fresh.setAdminNote(note);
        fresh.setResolvedAt(Instant.now());
        complaintRepository.save(fresh);
        DailyQuizAttempt attempt = fresh.getAnswer().getAttempt();
        attempt.setReviewResultUnseen(true);
        attemptRepository.save(attempt);
    }
}
