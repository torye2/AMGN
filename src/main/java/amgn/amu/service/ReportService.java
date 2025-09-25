package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import amgn.amu.common.LoginUser;
import amgn.amu.component.UserDirectory;
import amgn.amu.dto.ReportDtos;
import amgn.amu.entity.Report;
import amgn.amu.entity.ReportAction;
import amgn.amu.entity.ReportEvidence;
import amgn.amu.entity.UserSuspension;
import amgn.amu.repository.ReportActionRepository;
import amgn.amu.repository.ReportEvidenceRepository;
import amgn.amu.repository.ReportRepository;
import amgn.amu.repository.UserSuspensionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportEvidenceRepository evidenceRepository;
    private final ReportRepository reportRepository;
    private final ReportActionRepository actionRepository;
    private final UserSuspensionRepository suspensionRepository;
    private final UserDirectory userDirectory;
    private final LoginUser  loginUser;

    @Transactional
    public ReportDtos.CreateReportResponse createReport(ReportDtos.CreateReportRequest req, HttpServletRequest request) {
        Long reporterId = loginUser.userId(request);
        Long reportedUserId = userDirectory.findUserIdByNickNameOrThrow(req.reportedNickname());

        if (reportedUserId == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }

        if (reporterId.equals(reportedUserId)) {
            throw new AppException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .listingId(req.listingId())
                .chatRoomId(req.chatRoomId())
                .reasonCode(req.reasonCode())
                .reasonText(req.reasonText())
                .description(req.description())
                .status(Report.ReportStatus.PENDING)
                .evidenceCount(0)
                .build();

        reportRepository.save(report);
        return new ReportDtos.CreateReportResponse(report.getReportId());
    }

    @Transactional
    public void addEvidence(Long reportId, ReportDtos.AddEvidenceRequest req, HttpServletRequest request) {
        Long uid = loginUser.userId(request);
        var evidence = ReportEvidence.builder()
                .reportId(reportId)
                .filePath(req.filePath())
                .mimeType(req.mimeType())
                .fileSize(req.fileSize())
                .uploadedBy(uid)
                .build();
        evidenceRepository.save(evidence);

        long cnt = evidenceRepository.countByReportId(reportId);
        var report = reportRepository.findById(reportId).orElse(null);
        report.setEvidenceCount((int) cnt);
        reportRepository.save(report);
    }

    @Transactional
    public void uploadEvidenceFiles(Long reportId, MultipartFile[] files, HttpServletRequest request) throws IOException {
        Long uid = loginUser.userId(request);

        if (files == null || files.length == 0) return;

        var report = reportRepository.findById(reportId).orElse(null);

        Path baseDir = Paths.get("C:/amu/uploads/reports");
        LocalDate today = LocalDate.now();
        Path dayDir = baseDir.resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(String.format("%02d", today.getDayOfMonth()));
        Files.createDirectories(dayDir);

        int saved = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            if (!file.getContentType().startsWith("image/")) continue;
            if (file.getSize() > 5 * 1024 * 1024) continue;

            String original = file.getOriginalFilename();
            String safeName = (original == null ? "image" : original).replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileName = UUID.randomUUID() + "_" + safeName;

            Path savePath = dayDir.resolve(fileName);
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath.toFile());

            String url = "/uploads/reports/"
                    + today.getYear() + "/"
                    + String.format("%02d", today.getMonthValue()) + "/"
                    + String.format("%02d", today.getDayOfMonth()) + "/"
                    + fileName;

            var evidence = ReportEvidence.builder()
                    .reportId(reportId)
                    .filePath(url)
                    .mimeType(file.getContentType())
                    .fileSize((int)file.getSize())
                    .uploadedBy(uid)
                    .build();
            evidenceRepository.save(evidence);
            saved++;

            if (saved > 0) {
                long cnt = evidenceRepository.countByReportId(reportId);
                report.setEvidenceCount((int) cnt);
                reportRepository.save(report);
            }
        }
    }

    @Transactional
    public void addAction(Long reportId, ReportDtos.AddActionRequest req, HttpServletRequest request) {
        Long adminId = loginUser.userId(request);

        var action = ReportAction.builder()
                .reportId(reportId)
                .actorUserId(adminId)
                .actionType(req.actionType())
                .actionPayload(req.actionPayload())
                .comment(req.comment())
                .build();
        actionRepository.save(action);

        if (req.actionType() == ReportAction.ActionType.ASSIGN || req.actionType() == ReportAction.ActionType.NOTE) {
            var report = reportRepository.findById(reportId).orElse(null);
            if (report != null && report.getStatus() == Report.ReportStatus.PENDING) {
                report.setStatus(Report.ReportStatus.IN_REVIEW);
                report.setHandledBy(adminId);
                report.setHandledAt(Instant.now());
                reportRepository.save(report);
            }
        } else if (req.actionType() == ReportAction.ActionType.REJECT || req.actionType() == ReportAction.ActionType.RESOLVE) {
            var report = reportRepository.findById(reportId).orElse(null);
            report.setStatus(req.actionType() == ReportAction.ActionType.REJECT ? Report.ReportStatus.REJECTED : Report.ReportStatus.RESOLVED);
            report.setHandledBy(adminId);
            report.setHandledAt(Instant.now());
            reportRepository.save(report);
        }
    }

    @Transactional
    public void suspendFromReport(Long reportId, ReportDtos.SuspendUserRequest req, HttpServletRequest request) {
        Long adminId = loginUser.userId(request);

        var report = reportRepository.findById(reportId).orElse(null);
        Instant endAt = (req.days() == null || req.days() <= 0) ?
                null : Instant.now().plusSeconds(req.days() * 24L * 3600L);

        if (report != null) {
            var suspension = UserSuspension.builder()
                    .userId(report.getReportedUserId())
                    .reportId(report.getReportId())
                    .endAt(endAt)
                    .reasonCode(report.getReasonCode())
                    .reasonText(req.reasonText() != null ? req.reasonText() : report.getReasonText())
                    .createdBy(adminId)
                    .status(UserSuspension.SuspensionStatus.ACTIVE)
                    .build();
            suspensionRepository.save(suspension);

            userDirectory.setUserStatusBanned(report.getReportedUserId());

            report.setStatus(Report.ReportStatus.RESOLVED);
            report.setHandledBy(adminId);
            report.setHandledAt(Instant.now());
            reportRepository.save(report);
        }

        actionRepository.save(ReportAction.builder()
                .reportId(reportId)
                .actorUserId(adminId)
                .actionType(ReportAction.ActionType.SUSPEND)
                .actionPayload(endAt == null ? "{\"days\":0}" : "{\"days\":" + req.days() + "}")
                .comment("정지 처리 실행")
                .build());
    }

    @Transactional
    public void revokeSuspension(Long suspensionId, String reason, HttpServletRequest request) {
        Long adminId = loginUser.userId(request);
        var suspension = suspensionRepository.findById(suspensionId).orElse(null);
        if (suspension != null) {
            suspension.setStatus(UserSuspension.SuspensionStatus.REVOKED);
            suspension.setRevokedAt(Instant.now());
            suspension.setRevokeReason(reason);
            suspensionRepository.save(suspension);
        }

        boolean stillHasActive = suspensionRepository.existsByUserIdAndStatusAndEndAtAfterOrEndAtIsNull(
                suspension.getUserId(), UserSuspension.SuspensionStatus.ACTIVE, Instant.now()
        );
        if (!stillHasActive) userDirectory.setUserStatusActive(suspension.getUserId());
    }

    @Transactional
    public Page<ReportDtos.ReportListItem> listReports(Report.ReportStatus status, Pageable pageable) {
        var page = (status == null)
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return page.map(r -> new ReportDtos.ReportListItem(
                r.getReportId(), r.getReporterId(), r.getReportedUserId(), r.getListingId(),
                r.getReasonCode(), r.getReasonText(), r.getStatus(), r.getCreatedAt()
        ));
    }

    @Transactional
    public ReportDtos.ReportDetail getReportDetail(Long reportId) {
        var r = reportRepository.findById(reportId).orElse(null);
        var evidence = evidenceRepository.findByReportIdOrderByCreatedAtDesc(reportId).stream()
                .map(e -> new ReportDtos.ReportDetail.EvidenceItem(
                        e.getEvidenceId(), e.getFilePath(), e.getMimeType(), e.getFileSize(), e.getUploadedBy(), e.getCreatedAt()
                )).toList();
        var actions = actionRepository.findByReportIdOrderByCreatedAtAsc(reportId).stream()
                .map(a -> new ReportDtos.ReportDetail.ActionItem(
                        a.getActionId(), a.getActorUserId(), a.getActionType(), a.getActionPayload(), a.getComment(), a.getCreatedAt()
                )).toList();
        return new ReportDtos.ReportDetail(
                r.getReportId(), r.getReporterId(), r.getReportedUserId(), r.getListingId(), r.getChatRoomId(),
                r.getReasonCode(), r.getReasonText(), r.getDescription(), r.getStatus(), r.getEvidenceCount(),
                r.getHandledBy(), r.getHandledAt(), r.getCreatedAt(), r.getUpdatedAt(), evidence, actions
        );
    }
}
