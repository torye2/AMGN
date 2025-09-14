package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.dto.ShopInfoResponse;
import amgn.amu.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    public ShopInfoResponse getShopInfo(Long sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        long productCount = em.createQuery(
                        "select count(l) from Listing l where l.sellerId = :sellerId", Long.class)
                .setParameter("sellerId", sellerId)
                .getSingleResult();

        String intro = null;
        try {
            Object introObj = em.createNativeQuery(
                            "select intro from user_profile where user_id = :userId")
                    .setParameter("userId", sellerId)
                    .getSingleResult();
            if (introObj != null) {
                intro = String.valueOf(introObj);
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
                createdAtStr,
                daysSinceOpen,
                intro
        );
    }
}
