package amgn.amu.common;

public class AppException extends RuntimeException {
    private final ErrorCode code;
    public AppException(ErrorCode code){ super(code.message); this.code=code; }
    public AppException(ErrorCode code, String detail){ super(detail); this.code=code; }

    public AppException(ErrorCode code, Throwable cause) { super(code.message, cause); this.code=code; }
    public AppException(ErrorCode code, String detail, Throwable cause) { super(detail, cause); this.code=code; }

    public ErrorCode getCode(){ return code; }
}
