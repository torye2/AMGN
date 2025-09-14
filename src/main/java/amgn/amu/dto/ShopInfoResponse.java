package amgn.amu.dto;

public record ShopInfoResponse(
        Long userId,
        String userName,
        long productCount,
        String createdAt,     // ISO-8601 (yyyy-MM-dd)로 반환
        long daysSinceOpen,
        String intro
) {}
