package amgn.amu.controller;

import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.PaymentResponse;
import amgn.amu.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private Long getUserId(HttpSession session) {
        var loginUser = (amgn.amu.dto.LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) throw new RuntimeException("로그인이 필요합니다.");
        return loginUser.getUserId();
    }

    // 결제 준비 (merchant_uid 발급 등)
    @PostMapping("/prepare")
    public PaymentResponse preparePayment(@RequestBody PaymentRequest req, HttpSession session) {
        Long userId = getUserId(session);
        return paymentService.preparePayment(userId, req);
    }

    // 결제 완료 확인 (서버에서 금액 검증)
    @PostMapping("/complete")
    public PaymentResponse completePayment(@RequestBody PaymentRequest req, HttpSession session) {
        Long userId = getUserId(session);
        return paymentService.completePayment(userId, req);
    }
}
