package amgn.amu.dto;

import amgn.amu.entity.Report;
import amgn.amu.entity.ReportAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class ReportDtos {
    public record CreateReportRequest(
            @NotBlank String reportedNickname,
            Long listingId,
            Long chatRoomId,
            @NotNull Report.ReasonCode reasonCode,
            String reasonText,
            @NotBlank String description
            ) {}

    public record CreateReportResponse(Long reportId){}

    public record AddEvidenceRequest(
            @NotBlank String filePath,
            String mimeType,
            Integer fileSize
    ) {}

    public record ReportListItem(
       Long reportId,
       Long reporterId,
       Long reportedUserId,
       Long listingId,
       Report.ReasonCode reasonCode,
       String reasonText,
       Report.ReportStatus status,
       Instant createdAt
    ) {}

    public record ReportDetail(
            Long reportId,
            Long reporterId,
            Long reportedUserId,
            Long listingId,
            Long chatRoomId,
            Report.ReasonCode reasonCode,
            String reasonText,
            String description,
            Report.ReportStatus status,
            Integer evidenceCount,
            Long handledBy,
            Instant handledAt,
            Instant createdAt,
            Instant updatedAt,
            List<EvidenceItem> evidence,
            List<ActionItem> actions
    ) {
        public record EvidenceItem(Long evidenceId, String filePath, String mimeType, Integer fileSize, Long uploadedBy, Instant createdAt) {}
        public record ActionItem(Long actionId, Long actorUserId, ReportAction.ActionType actionType, String actionPayload, String comment, Instant createdAt) {}
    }

    public record AddActionRequest(
            @NotNull ReportAction.ActionType actionType,
            String actionPayload,
            String comment
    ) {}

    public record SuspendUserRequest(
            @Min(0) Integer days, // 0이면 영구 정지
            String reasonText
    ) {}
}
