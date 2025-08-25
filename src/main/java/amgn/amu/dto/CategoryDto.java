package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {

	private Long category_id ;
	private String name        ;
	private Long parent_id   ;
	private String path        ;
}
