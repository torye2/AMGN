// src/main/java/amgn/amu/entity/Region.java
package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regions") // 실제 테이블명이 'regions'면 이렇게, 'region'이면 "region"으로
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "region_id")   // DB: region_id
	private Long id;

	@Column(nullable = false)     // DB: name
	private String name;

	@Column(name = "parent_id")   // DB: parent_id
	private Long parentId;

	@Column(name = "level_no")    // DB: level_no (TINYINT 가능)
	private Integer levelNo;

	@Column(length = 400)         // DB: path
	private String path;
}
