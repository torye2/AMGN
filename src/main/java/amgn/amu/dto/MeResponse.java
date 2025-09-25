package amgn.amu.dto;

import java.util.List;

public record MeResponse(
        boolean loggedIn,
        Long userId,
        String nickname,
        List<String> roles,
        boolean isAdmin
) { }
