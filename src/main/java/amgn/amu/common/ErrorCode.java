package amgn.amu.common;

public enum ErrorCode {
    NOT_FOUND_USER(404, "일치하는 회원 정보를 찾을 수 없습니다."),
    DUPLICATE_ID(400, "이미 사용 중인 아이디입니다."),
    DUPLICATE_PW(400, "이미 사용 중인 비밀번호입니다."),
    MATCH_PW(400, "비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(400, "이미 가입된 이메일입니다."),
    DUPLICATE_PHONE(400, "이미 가입된 전화번호입니다."),
    VERIFICATION_RATE_LIMIT(429, "인증코드는 1분에 1회만 요청할 수 있어요."),
    VERIFICATION_NOT_FOUND(400, "유효한 인증코드가 없습니다."),
    VERIFICATION_MISMATCH(400, "인증코드가 올바르지 않습니다."),
    ADDRESS_NOT_FOUND(400, "주소를 찾을 수 없습니다."),
    BAD_REQUEST(400, "잘못된 요청입니다."),
    SERVER_ERROR(500, "서버 오류가 발생했습니다."),
    RESET_TOKEN_INVALID(400, "유효하지 않거나 만료된 토큰입니다."),
    PASSWORD_POLICY(400, "비밀번호 규칙을 확인해 주세요."),
    RATE_LIMIT(429, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "대상을 찾을 수 없습니다.");
    public final int status; public final String message;
    ErrorCode(int s, String m){ this.status=s; this.message=m; }
}
