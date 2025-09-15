// src/main/java/amgn/amu/entity/Category.java
package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")           // 실제 테이블명 확인
@Access(AccessType.FIELD)             // ★ 필드 접근만 사용
@Getter @Setter
@NoArgsConstructor
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	//@Column(name = "parent_id")
	private Long parentId;

	@Column(length = 400)
	private String path;
}
