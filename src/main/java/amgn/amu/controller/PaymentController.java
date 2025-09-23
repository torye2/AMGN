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

    @PostMapping("/prepare")
    public PaymentResponse preparePayment(@RequestBody PaymentRequest req, HttpSession session) {
        Long userId = getUserId(session);
        return paymentService.preparePayment(userId, req.orderId());
    }

    @PostMapping("/complete")
    public PaymentResponse completePayment(@RequestBody PaymentRequest req, HttpSession session) {
        Long userId = getUserId(session);
        return paymentService.completePayment(userId, req);
    }

    @GetMapping("/payment/test-pre-register/{orderId}/{amount}")
    public String testPreRegister(@PathVariable Long orderId, @PathVariable Long amount) {
        return paymentService.testPreRegister(orderId, amount);
    }
}
