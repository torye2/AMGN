package amgn.amu.dto;

import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingPhoto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record ListingSummaryResponse(
        Long id,
        String title,
        BigDecimal price,
        String currency,
        Integer categoryId,
        Integer regionId,
        String thumbnailUrl,
        Timestamp createdAt,
        Integer wishCount,
        Integer viewCount
) {
    public static ListingSummaryResponse from(Listing l) {
        String thumb = null;
        if (l.getPhotos() != null && !l.getPhotos().isEmpty()) {
            ListingPhoto p = l.getPhotos().get(0);
            // ↓ 프로젝트의 ListingPhoto 필드명에 맞춰 한 줄만 바꿔주세요
            // 예: p.getUrl() / p.getPhotoUrl() / p.getPath() / p.getStoredName()
            thumb = /* p.getUrl() */ null;
        }
        return new ListingSummaryResponse(
                l.getListingId(),
                l.getTitle(),
                l.getPrice(),
                l.getCurrency(),
                l.getCategoryId(),
                l.getRegionId(),
                thumb,
                l.getCreatedAt(),
                l.getWishCount(),
                l.getViewCount()
        );
    }
}
