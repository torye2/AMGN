package amgn.amu.controller;

import amgn.amu.dto.ReportDtos;
import amgn.amu.entity.Report;
import amgn.amu.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ReportDtos.CreateReportResponse create(@RequestBody @Valid ReportDtos.CreateReportRequest req, HttpServletRequest request) {
        return reportService.createReport(req, request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ReportDtos.ReportListItem> list(@RequestParam(required = false) Report.ReportStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return reportService.listReports(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportDtos.ReportDetail detail(@PathVariable Long id){
        return reportService.getReportDetail(id);
    }

    @PostMapping("{id}/evidence")
    @PreAuthorize("isAuthenticated()")
    public void addEvidence(@PathVariable Long id, @RequestBody @Valid ReportDtos.AddEvidenceRequest req, HttpServletRequest request) {
        reportService.addEvidence(id, req, request);
    }

    @PostMapping("{id}/actions")
    @PreAuthorize("hasRole('ADMIN')")
    public void addAction(@PathVariable Long id, @RequestBody @Valid ReportDtos.AddActionRequest req, HttpServletRequest request) {
        reportService.addAction(id, req, request);
    }

    @PostMapping("{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public void suspend(@PathVariable Long id, @RequestBody @Valid ReportDtos.SuspendUserRequest req, HttpServletRequest request) {
        reportService.suspendFromReport(id, req, request);
    }
}
