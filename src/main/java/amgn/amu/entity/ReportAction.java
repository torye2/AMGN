package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "report_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(nullable = false)
    private Long reportId;

    @Column(nullable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionType actionType;

    @Lob
    private String actionPayload; // 운영/보관용 JSON 문자열

    @Lob
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public enum ActionType { NOTE, ASSIGN, REQUEST_INFO, SUSPEND, REJECT, RESOLVE, REVOKE_SUSPENSION }
}

