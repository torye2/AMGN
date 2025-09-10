package amgn.amu.mapper;

import amgn.amu.entity.UserMfaTotp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMfaTotpMapper {
    Optional<UserMfaTotp> get(@Param("userId") Long userId);
    int upsert(@Param("userId") Long userId, @Param("secretEnc") byte[] secretEnc,
               @Param("enabled") boolean enabled);
    int setEnabled(@Param("userId") Long userId, @Param("enabled") boolean enabled);
}
