package amgn.amu.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class FlashMsgController {

    @GetMapping("/flash")
    public ResponseEntity<Map<String,Object>> getFlashMsg(HttpServletRequest req){
        HttpSession session = req.getSession(false);
        String msg = (session != null) ? (String) session.getAttribute("FLASH_MSG") : null;
        if (session != null) {
            session.removeAttribute("FLASH_MSG");
        }
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", msg
        ));
    }
}
