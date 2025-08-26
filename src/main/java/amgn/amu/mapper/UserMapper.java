package amgn.amu.mapper;

import amgn.amu.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    boolean existsById(@Param("id") String id);
    boolean existsByEmail(@Param("email") String email);
    boolean existsByPhone(@Param("phoneNumber") String phoneNumber);


    Optional<User> findById(@Param("id") String id);

    int insert(User user);
}
