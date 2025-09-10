package amgn.amu.mapper;

import amgn.amu.entity.UserMfaBackupCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMfaBackupCodeMapper {
    int insertBatch(@Param("userId") Long userId, @Param("codes") List<UserMfaBackupCode> codes);
    List<UserMfaBackupCode> listActive(@Param("userId") Long userId);
    int markUsed(@Param("id") Long id);
}
