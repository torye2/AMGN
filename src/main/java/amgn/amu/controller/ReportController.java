package amgn.amu.controller;

import amgn.amu.common.CustomUserDetails;
import amgn.amu.dto.ReportDtos;
import amgn.amu.entity.Report;
import amgn.amu.entity.UserSuspension;
import amgn.amu.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    record RevokeRequest(String reason) {}
    private final ReportService reportService;

    @PostMapping(value = "/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReportDtos.CreateReportResponse create(@RequestBody @Valid ReportDtos.CreateReportRequest req, HttpServletRequest request) {
        return reportService.createReport(req, request);
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ReportDtos.ReportListItem> list(@RequestParam(required = false) Report.ReportStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return reportService.listReports(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/admin/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportDtos.ReportDetail detail(@PathVariable Long id){
        return reportService.getReportDetail(id);
    }

    @PostMapping("/reports/{id}/evidence")
    @PreAuthorize("isAuthenticated()")
    public void addEvidence(@PathVariable Long id, @RequestBody @Valid ReportDtos.AddEvidenceRequest req, HttpServletRequest request) {
        reportService.addEvidence(id, req, request);
    }

    @PostMapping(value = "/reports/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public void uploadEvidence(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files, HttpServletRequest request) throws IOException {
        reportService.uploadEvidenceFiles(id, files.toArray(MultipartFile[]::new), request);
    }

    @PostMapping("/admin/reports/{id}/actions")
    @PreAuthorize("hasRole('ADMIN')")
    public void addAction(@PathVariable Long id, @RequestBody @Valid ReportDtos.AddActionRequest req, HttpServletRequest request) {
        reportService.addAction(id, req, request);
    }

    @PostMapping("/admin/reports/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public void suspend(@PathVariable Long id, @RequestBody @Valid ReportDtos.SuspendUserRequest req, HttpServletRequest request) {
        reportService.suspendFromReport(id, req, request);
    }

    @GetMapping("/admin/users/{userId}/suspensions")
    public List<UserSuspension> list(@PathVariable Long userId,
                                     @RequestParam(defaultValue = "false") boolean active) {
        return reportService.listUserSuspensions(userId, active);
    }

    @PostMapping("/admin/suspensions/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable("id") Long suspensionId,
                                       @RequestBody RevokeRequest req,
                                       HttpServletRequest request) {
        reportService.revokeSuspension(suspensionId, req != null ? req.reason() : null, request);
        return ResponseEntity.noContent().build();
    }
}
