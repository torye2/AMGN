package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
public class Inquiry {

    public enum Status {
        PENDING, ANSWERED, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    @Column(name = "user_id")
    private Long userId; // 로그인 사용자 없으면 null 저장

    @Column(name = "title", length = 200, nullable = true)
    private String title;

    @Lob
    @Column(name = "content", nullable = true)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        var now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.status == null) this.status = Status.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
