package amgn.amu.repository;

import amgn.amu.entity.UserSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {
    List<UserSuspension> findByUserIdAndStatus(Long userId, UserSuspension.SuspensionStatus status);
    boolean existsByUserIdAndStatusAndEndAtAfterOrEndAtIsNull(Long userId, UserSuspension.SuspensionStatus status, Instant now);
}
