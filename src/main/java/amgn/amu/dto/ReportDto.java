package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDto {

	private Long report_id    ;
	private Long reporter_id  ;
	private String target_type  ;
	private Long target_id    ;
	private String reason       ;
	private String status       ;
	private int created_at   ;
}
