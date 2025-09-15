package amgn.amu.common;

public enum ErrorCode {
    NOT_FOUND_USER(404, "일치하는 회원 정보를 찾을 수 없습니다."),
    NOT_LOGGED_IN(401, "로그인 되어있는 회원 정보를 찾을 수 없습니다."),
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
    NOT_FOUND(404, "대상을 찾을 수 없습니다."),
    MFA_CRYPTO_ERROR(500, "보안 키 처리 중 오류가 발생했습니다."),
    MFA_QR_GENERATION_FAILED(500, "QR 코드를 생성하지 못했습니다."),
    MFA_CODE_INVALID(400, "인증 코드가 올바르지 않습니다."),
    MFA_NOT_ENABLED(409, "2단계 인증이 설정되어 있지 않습니다."),
    MFA_REQUIRED(401, "추가 인증이 필요합니다."),
    SECURITY_MISSING_ENV(500, "서버 보안 설정이 누락되었습니다."),
    OAUTH_PROVIDER_ERROR(502, "소셜 로그인 제공자와 통신에 실패했습니다."),
    OAUTH_UNSUPPORTED_PROVIDER(400, "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_LINK_FAILED(500, "소셜 계정 연결에 실패했습니다."),
    OAUTH_NOT_FOUND(404, "해당 제공자 연결이 존재하지 않습니다: ");
    public final int status; public final String message;
    ErrorCode(int s, String m){ this.status=s; this.message=m; }
}
