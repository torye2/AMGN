package amgn.amu.controller;

import amgn.amu.common.LoginUser;
import amgn.amu.dto.AddressDto;
import amgn.amu.dto.AddressRequest;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.service.UserAddressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressService service;
    private final LoginUser loginUser;

    private Long currentUserId(HttpServletRequest req) {
        Long userId = loginUser.userId(req);
        if (userId == null) throw new RuntimeException("로그인 필요");
        return (Long) userId;
    }

    @GetMapping
    public ResponseEntity<List<AddressDto>> list(HttpServletRequest req) {
        Long userId = currentUserId(req);
        return ResponseEntity.ok(service.list(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDto> get(@PathVariable Long id, HttpServletRequest req) {
        Long userId = currentUserId(req);
        return ResponseEntity.ok(service.get(userId, id));
    }

    @PostMapping
    public ResponseEntity<AddressDto> create(@RequestBody AddressRequest body, HttpServletRequest req) {
        Long userId = currentUserId(req);
        return ResponseEntity.ok(service.create(userId, body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> update(@PathVariable Long id,
                                             @RequestBody AddressRequest body,
                                             HttpServletRequest req) {
        Long userId = currentUserId(req);
        return ResponseEntity.ok(service.update(userId, id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest req) {
        Long userId = currentUserId(req);
        service.delete(userId, id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<?> setDefault(@PathVariable Long id, HttpServletRequest req) {
        Long userId = currentUserId(req);
        service.setDefault(userId, id);
        return ResponseEntity.ok().build();
    }
}

