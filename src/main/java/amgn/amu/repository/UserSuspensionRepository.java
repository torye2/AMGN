package amgn.amu.repository;

import amgn.amu.entity.UserSuspension;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, Long> {
    List<UserSuspension> findByUserIdAndStatus(Long userId, UserSuspension.SuspensionStatus status);
    boolean existsByUserIdAndStatusAndEndAtAfterOrEndAtIsNull(Long userId, UserSuspension.SuspensionStatus status, Instant now);

    @Query("""
      select s from UserSuspension s
      where s.userId = :userId
      and (:activeOnly = false or (s.status = 'ACTIVE' and (s.endAt is null or s.endAt > CURRENT_TIMESTAMP)))
      order by s.createdAt desc
    """)
    List<UserSuspension> findByUserId(@Param("userId") Long userId, @Param("activeOnly") boolean activeOnly);
}
