package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "block")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    @EmbeddedId
    private PK id;

    @Column(name = "created_at")
    private int createdAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        @Column(name = "blocker_id")
        private Long blockerId;

        @Column(name = "blocked_id")
        private Long blockedId;
    }
}
