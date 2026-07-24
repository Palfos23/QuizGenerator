package com.quizapp.service;

import com.quizapp.dto.ReportDto;
import com.quizapp.dto.ResolveReportRequest;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.Report;
import com.quizapp.model.ReportStatus;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final AppUserRepository appUserRepository;

    public ReportService(ReportRepository reportRepository, AppUserRepository appUserRepository) {
        this.reportRepository = reportRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public ReportDto submit(String userEmail, ReportDto dto) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for " + userEmail));

        Report report = new Report();
        report.setArea(dto.getArea());
        report.setMessage(dto.getMessage());
        report.setReportedBy(user);
        report.setStatus(ReportStatus.OPEN);

        return toDto(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportDto> listMine(String userEmail) {
        return reportRepository.findByReportedBy_EmailOrderByCreatedAtDesc(userEmail)
                .stream().map(ReportService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportDto> listAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(ReportService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ReportDto resolve(Long id, ResolveReportRequest request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No report found with id " + id));
        report.setStatus(ReportStatus.RESOLVED);
        report.setAdminNote(request.getAdminNote());
        return toDto(reportRepository.save(report));
    }

    static ReportDto toDto(Report r) {
        ReportDto dto = new ReportDto();
        dto.setId(r.getId());
        dto.setArea(r.getArea());
        dto.setMessage(r.getMessage());
        dto.setStatus(r.getStatus());
        dto.setAdminNote(r.getAdminNote());
        dto.setReporterName(r.getReportedBy().getName());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
