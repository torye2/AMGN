package amgn.amu.controller;

import amgn.amu.service.CheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserCheckController {

    private final CheckService checkService;

    @GetMapping("/exist")
    public ResponseEntity<Map<String, Boolean>> checkId(@RequestParam("id") String id) {
        boolean exist = checkService.extistById(id);
        return ResponseEntity.ok(Map.of("exist", exist));
    }
}
