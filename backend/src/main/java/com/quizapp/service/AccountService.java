package com.quizapp.service;

import com.quizapp.dto.AccountExportDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.Report;
import com.quizapp.model.SubmittedQuestion;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.LineupAttemptRepository;
import com.quizapp.repository.ReportRepository;
import com.quizapp.repository.SavedQuizRepository;
import com.quizapp.repository.SubmittedQuestionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Self-service GDPR rights - "download my data" and "delete my account". See
// the plan doc for why submitted questions/reports are anonymized rather than
// deleted outright (they're shared content other people already see/reviewed)
// while saved quizzes and play history, which are genuinely personal, are
// deleted along with the account itself.
@Service
public class AccountService {

    private final AppUserRepository appUserRepository;
    private final SavedQuizRepository savedQuizRepository;
    private final SubmittedQuestionRepository submittedQuestionRepository;
    private final ReportRepository reportRepository;
    private final GridAttemptRepository gridAttemptRepository;
    private final LineupAttemptRepository lineupAttemptRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository appUserRepository,
                           SavedQuizRepository savedQuizRepository,
                           SubmittedQuestionRepository submittedQuestionRepository,
                           ReportRepository reportRepository,
                           GridAttemptRepository gridAttemptRepository,
                           LineupAttemptRepository lineupAttemptRepository,
                           PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.savedQuizRepository = savedQuizRepository;
        this.submittedQuestionRepository = submittedQuestionRepository;
        this.reportRepository = reportRepository;
        this.gridAttemptRepository = gridAttemptRepository;
        this.lineupAttemptRepository = lineupAttemptRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AccountExportDto exportData(String email) {
        AppUser user = requireUser(email);

        AccountExportDto dto = new AccountExportDto();
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setGoogleLinked(user.getGoogleSubject() != null);
        dto.setCreatedAt(user.getCreatedAt());

        dto.setSavedQuizzes(savedQuizRepository.findByOwner_EmailOrderByCreatedAtDesc(email).stream()
                .map(q -> new AccountExportDto.SavedQuizExport(q.getTitle(), q.getLanguage().name(), q.getCreatedAt()))
                .collect(Collectors.toList()));

        dto.setSubmittedQuestions(submittedQuestionRepository.findBySubmittedBy_EmailOrderByCreatedAtDesc(email).stream()
                .map(q -> new AccountExportDto.SubmittedQuestionExport(
                        q.getQuestionText(), q.getAnswer(), q.getCategory(), q.getStatus().name(), q.getCreatedAt()))
                .collect(Collectors.toList()));

        dto.setReports(reportRepository.findByReportedBy_EmailOrderByCreatedAtDesc(email).stream()
                .map(r -> new AccountExportDto.ReportExport(r.getArea(), r.getMessage(), r.getStatus().name(), r.getCreatedAt()))
                .collect(Collectors.toList()));

        dto.setGridAttempts(gridAttemptRepository.findByUser_Email(email).stream()
                .map(a -> new AccountExportDto.GridAttemptExport(a.getGrid().getTitle(), a.isCompleted(), a.getStrikesUsed()))
                .collect(Collectors.toList()));

        dto.setLineupAttempts(lineupAttemptRepository.findByUser_Email(email).stream()
                .map(a -> new AccountExportDto.LineupAttemptExport(a.getLineup().getTitle(), a.isCompleted(), a.getStrikesUsed()))
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public void deleteAccount(String email, String password) {
        AppUser user = requireUser(email);

        if (user.getPasswordHash() != null) {
            if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new IllegalArgumentException("Incorrect password.");
            }
        }

        // Anonymize contributions to shared content instead of deleting it -
        // other people may already be seeing/reviewing this.
        List<SubmittedQuestion> submitted = submittedQuestionRepository
                .findBySubmittedBy_EmailOrderByCreatedAtDesc(email);
        submitted.forEach(q -> q.setSubmittedBy(null));
        submittedQuestionRepository.saveAll(submitted);

        List<Report> reports = reportRepository.findByReportedBy_EmailOrderByCreatedAtDesc(email);
        reports.forEach(r -> r.setReportedBy(null));
        reportRepository.saveAll(reports);

        // Delete genuinely personal data outright.
        savedQuizRepository.deleteAll(savedQuizRepository.findByOwner_EmailOrderByCreatedAtDesc(email));
        gridAttemptRepository.deleteAll(gridAttemptRepository.findByUser_Email(email));
        lineupAttemptRepository.deleteAll(lineupAttemptRepository.findByUser_Email(email));

        appUserRepository.delete(user);
    }

    private AppUser requireUser(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for " + email));
    }
}
