package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상점 프로필(소개/이미지) 업서트 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopProfileDto {
    private String intro;
    private String profileImg; // 정적 접근 URL (/uploads/..)
}
