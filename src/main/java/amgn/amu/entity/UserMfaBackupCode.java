package amgn.amu.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class UserMfaBackupCode {
    private Long id;
    private Long userId;
    private String codeHash;
    private boolean used;
    private LocalDateTime createdAt;
}

