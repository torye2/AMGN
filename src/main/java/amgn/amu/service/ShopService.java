package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.dto.ShopInfoResponse;
import amgn.amu.dto.ShopProfileDto;
import amgn.amu.repository.UserRepository;
import amgn.amu.service.util.UploadPathProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final UserRepository userRepository;
    private final UploadPathProvider uploadPathProvider;

    @PersistenceContext
    private EntityManager em;

    public ShopInfoResponse getShopInfo(Long sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        long productCount = em.createQuery(
                        "select count(l) from Listing l where l.sellerId = :sellerId", Long.class)
                .setParameter("sellerId", sellerId)
                .getSingleResult();

        // SOLD 상태 개수
        long soldCount = em.createQuery(
                        "select count(l) from Listing l " +
                                "where l.sellerId = :sellerId and upper(trim(l.status)) = 'SOLD'",
                        Long.class)
                .setParameter("sellerId", sellerId)
                .getSingleResult();

        String intro = null;
        String profileImg = null;
        try {
            Object[] row = (Object[]) em.createNativeQuery(
                            "select intro, profile_img from user_profile where user_id = :userId")
                    .setParameter("userId", sellerId)
                    .getSingleResult();
            if (row != null) {
                intro = row[0] != null ? String.valueOf(row[0]) : null;
                profileImg = row[1] != null ? String.valueOf(row[1]) : null;
            }
        } catch (NoResultException ignore) {
        }

        // createdAt은 User 엔티티의 LocalDateTime 기준, 일수 계산
        LocalDate createdDate = user.getCreatedAt() != null
                ? user.getCreatedAt().toLocalDate()
                : null;

        long daysSinceOpen = createdDate != null
                ? ChronoUnit.DAYS.between(createdDate, LocalDate.now())
                : 0L;

        String createdAtStr = createdDate != null
                ? createdDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                : null;

        return new ShopInfoResponse(
                user.getUserId(),
                user.getUserName(),
                productCount,
                soldCount,
                createdAtStr,
                daysSinceOpen,
                intro,
                profileImg
        );
    }

    @Transactional
    public ShopProfileDto upsertUserProfile(Long userId, String intro, MultipartFile photo) {
        try {
            // intro null-safe 처리
            final String safeIntro = (intro != null) ? intro : "";

            // 이미지 저장(선택)
            String savedUrl = null;
            if (photo != null && !photo.isEmpty()) {
                String orig = photo.getOriginalFilename() == null ? "" : photo.getOriginalFilename();
                String ext = "";
                int i = orig.lastIndexOf('.');
                if (i > -1 && i < orig.length() - 1) ext = orig.substring(i + 1);
                String fileName = "profile_" + userId + "_" + UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);

                // UploadPathProvider가 제공하는 디렉터리에 저장
                Path uploadDir = uploadPathProvider.getUploadsDir();
                if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
                Path target = uploadDir.resolve(fileName);
                photo.transferTo(target.toFile());

                savedUrl = "/uploads/" + fileName; // 정적 경로
            }

            // 존재 여부 확인
            Number cnt = (Number) em.createNativeQuery("select count(*) from user_profile where user_id = :uid")
                    .setParameter("uid", userId)
                    .getSingleResult();
            boolean exists = cnt != null && cnt.longValue() > 0;

            if (exists) {
                if (savedUrl != null) {
                    em.createNativeQuery("update user_profile set intro = :intro, profile_img = :img where user_id = :uid")
                            .setParameter("intro", safeIntro)
                            .setParameter("img", savedUrl)
                            .setParameter("uid", userId)
                            .executeUpdate();
                } else {
                    em.createNativeQuery("update user_profile set intro = :intro where user_id = :uid")
                            .setParameter("intro", safeIntro)
                            .setParameter("uid", userId)
                            .executeUpdate();
                }
            } else {
                if (savedUrl != null) {
                    em.createNativeQuery("insert into user_profile (user_id, intro, profile_img) values (:uid, :intro, :img)")
                            .setParameter("uid", userId)
                            .setParameter("intro", safeIntro)
                            .setParameter("img", savedUrl)
                            .executeUpdate();
                } else {
                    em.createNativeQuery("insert into user_profile (user_id, intro) values (:uid, :intro)")
                            .setParameter("uid", userId)
                            .setParameter("intro", safeIntro)
                            .executeUpdate();
                }
            }

            return new ShopProfileDto(safeIntro, savedUrl);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 저장 실패", e);
        }
    }
}
