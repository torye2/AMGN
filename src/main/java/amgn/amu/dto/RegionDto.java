package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegionDto {

	private Long region_id ;
	private String name      ;
	private Integer level_no  ;
	private Long parent_id ;
	private String path      ;
}
