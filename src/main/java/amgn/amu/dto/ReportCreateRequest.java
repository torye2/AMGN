package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportCreateRequest {

	private String targetType; // USER, POST, COMMENT 등
    private Long targetId;
    private String reason;
}
