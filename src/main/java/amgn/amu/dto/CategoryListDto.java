package amgn.amu.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryListDto {
    private Long categoryId;
    private String name;
    private Long parentId;
    private String path;
}
