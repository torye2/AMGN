package amgn.amu.mapper;

import amgn.amu.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    boolean existsByUserId(@Param("userId") String userId);
    boolean existsByEmail(@Param("email") String email);
    boolean existsByPhone(@Param("phoneNumber") String phoneNumber);


    Optional<User> findByUserId(@Param("userId") String userId);

    int insert(User user);
}
