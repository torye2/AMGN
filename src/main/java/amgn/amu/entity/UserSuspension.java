package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "user_suspensions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuspension {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long suspensionId;

    @Column(nullable = false)
    private Long userId;
    private Long reportId;

    @Column(nullable = false)
    private Instant startAt;
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Report.ReasonCode reasonCode;

    @Column(length = 255)
    private String reasonText;

    @Column(nullable = false)
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuspensionStatus status;
    private Instant revokedAt;
    private String revokeReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() {
        var now = Instant.now();
        createdAt = now;
        if (startAt == null) startAt = now;
        if (status == null) status = SuspensionStatus.ACTIVE;
    }

    public enum SuspensionStatus { ACTIVE, REVOKED, EXPIRED }
}
