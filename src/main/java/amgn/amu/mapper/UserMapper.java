package amgn.amu.mapper;

import amgn.amu.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    boolean existsByLoginId(@Param("loginId") String id);

    boolean existsByEmail(@Param("email") String email);

    boolean existsByPhone(@Param("phoneNumber") String phoneNumber);

    Optional<User> findByLoginId(@Param("loginId") String id);

    int insert(User user);

    Optional<User> findByEmail(@Param("email") String email);

    int createSocialUser(User user);

    int completeOnboarding(@Param("userId") Long userId, @Param("phoneNumber") String phoneNumber, @Param("phoneE164") String phoneE164);

    Optional<User> findByEmailNormalized(@Param("emailNormalized") String emailNormalized);

    boolean existsByEmailNormalized(@Param("emailNormalized") String emailNormalized);

    boolean existsByPhoneE164(@Param("phoneE164") String phoneE164);
}
