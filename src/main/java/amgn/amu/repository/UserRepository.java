package amgn.amu.repository;

import amgn.amu.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 'id' 필드를 기준으로 사용자를 찾는 메서드
    Optional<User> findById(String id);
}