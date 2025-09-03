package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor      // JPA 기본 생성자
@AllArgsConstructor     // 모든 필드를 받는 생성자
public class CategoryList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    private String name;

    private Long parentId;

    private String path;
}
