package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.UploadsDiagnostics;
import amgn.amu.service.UploadsDiagnosticsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
public class SystemController {

    private final UploadsDiagnosticsService diagnosticsService;

    @GetMapping("/uploads/diagnose")
    public ResponseEntity<UploadsDiagnostics> diagnoseUploads(HttpSession session) {
        LoginUserDto login = (LoginUserDto) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(401).build();
        }
        UploadsDiagnostics result = diagnosticsService.diagnose();
        return ResponseEntity.ok(result);
    }
}
