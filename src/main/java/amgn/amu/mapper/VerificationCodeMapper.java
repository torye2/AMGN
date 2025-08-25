package amgn.amu.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

@Mapper
public interface VerificationCodeMapper {
    void create(@Param("channel") String channel, @Param("dest") String dest
                , @Param("code") String code, @Param("purpose") String purpose
                , @Param("expiresAt") OffsetDateTime expiresAt);

    Optional<String> findValidCode(@Param("channel") String channel, @Param("dest") String dest
                                    , @Param("purpose") String purpose);

    int markUsed(@Param("channel") String channel, @Param("dest") String dest
                ,  @Param("code") String code, @Param("purpose") String purpose);

    int countSendsLastMinute(@Param("channel") String channel, @Param("dest") String dest);
}
