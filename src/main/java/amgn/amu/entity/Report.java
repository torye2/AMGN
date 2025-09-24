package amgn.amu.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports")
@Data
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(nullable = false)
    private Long reportedUserId;

    private Long listingId;
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 20)
    private ReasonCode reasonCode;

    @Column(length = 255)
    private String reasonText;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false)
    private Integer evidenceCount;

    private Long handledBy;
    private Instant handledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ReportStatus.PENDING;
        if (evidenceCount == null) evidenceCount = 0;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public enum ReasonCode { ABUSE, SCAM, INAPPROPRIATE, OTHER }

    public enum ReportStatus { PENDING, IN_REVIEW, RESOLVED, REJECTED, CANCELED }
}
