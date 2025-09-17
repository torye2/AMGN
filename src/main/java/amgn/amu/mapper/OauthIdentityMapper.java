package amgn.amu.mapper;

import amgn.amu.domain.User;
import amgn.amu.entity.OauthIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OauthIdentityMapper {
    Optional<User> findUserByProvider(@Param("provider") String provider,
                                      @Param("pid") String providerUserId);
    Optional<OauthIdentity> findByProvider(@Param("provider") String provider,
                                           @Param("pid") String providerUserId);
    int insertLink(OauthIdentity link);
    int updateTokens(OauthIdentity link);
    List<String> findProvidersByUserId(@Param("userId") Long userId);
    int deleteLinkByUserAndProvider(@Param("userId") Long userId, @Param("provider") String provider);
    Long findUserIdByProviderAndPid(@Param("provider") String provider, @Param("pid") String pid);
}
