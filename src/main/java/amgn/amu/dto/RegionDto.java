package amgn.amu.dto;

import amgn.amu.entity.Region;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegionDto {

	// 서버 응답 JSON 키를 camelCase로 맞추기 위해 JsonProperty 부여
	@JsonProperty("regionId")
	private Long region_id;

	private String name;

	@JsonProperty("levelNo")
	private Integer level_no;

	@JsonProperty("parentId")
	private Long parent_id;

	private String path;

	/** 엔티티 → DTO 매핑용 정적 팩토리 */
	public static RegionDto from(Region e) {
		if (e == null) return null;
		RegionDto dto = new RegionDto();
		dto.setRegion_id(e.getId());  // 엔티티의 getter 이름에 맞춰서 매핑
		dto.setName(e.getName());
		dto.setLevel_no(e.getLevelNo());
		dto.setParent_id(e.getParentId());
		dto.setPath(e.getPath());
		return dto;
	}
}
