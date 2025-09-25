package amgn.amu.common;

import ch.qos.logback.classic.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public Object handleApp(AppException e){
        ErrorCode code = e.getCode();
        return ResponseEntity.status(code.status)
                .body(ApiResult.fail(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleBind(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors()
                .stream().findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("유효성 검사에 실패했습니다. 입력값이 올바르지 않습니다.");
        return ResponseEntity.status(400).body(ApiResult.fail(msg));
    }

    @ExceptionHandler(Exception.class)
    public Object handleEtc(Exception e){
        return ResponseEntity.status(500).body(ApiResult.fail("서버 오류가 발생했습니다."));
    }
}
