package amgn.amu.common;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public Object handleApp(AppException e, Model model){
        ErrorCode code = e.getCode();
        return ResponseEntity.status(code.status).body(Map.of(
           "error", code.name(), "message", e.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public Object handleEtc(Exception e){
        return ResponseEntity.status(500).body(Map.of(
           "error", "INTERNAL", "message", "서버 오류가 발생했습니다."
        ));
    }
}
