package amgn.amu.service.util;

public interface UserDirectory {
    Long findUserIdByNicknameOrThrow(String nickname);
    void setUserStatusBanned(Long userId);
    void setUserStatusActive(Long userId);
}
