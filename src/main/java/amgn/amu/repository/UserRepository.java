package amgn.amu.repository;

import amgn.amu.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 'id' 필드를 기준으로 사용자를 찾는 메서드
    Optional<User> findByLoginId(String id);

    Optional<User> findByUserId(Long userId);

    // 아이디 찾기: 이름/생년월일/휴대폰 + ACTIVE
    Optional<User> findByUserNameAndBirthYearAndBirthMonthAndBirthDayAndPhoneNumber(
            String userName, Integer birthYear, Integer birthMonth, Integer birthDay, String phoneNumber
    );

    // 비번 재설정 1단계: 로그인아이디 + 동일정보 + ACTIVE
    Optional<User> findByLoginIdAndUserNameAndBirthYearAndBirthMonthAndBirthDayAndPhoneNumber(
            String Id, String userName, Integer birthYear, Integer birthMonth, Integer birthDay, String phoneNumber
    );
}