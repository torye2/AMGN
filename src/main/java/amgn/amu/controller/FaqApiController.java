package amgn.amu.controller;

import amgn.amu.common.LoginUser;
import amgn.amu.domain.User;
import amgn.amu.dto.FaqCreateRequest;
import amgn.amu.dto.FaqSummaryDto;
import amgn.amu.entity.Faq;
import amgn.amu.repository.FaqRepository;
import amgn.amu.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FaqApiController {

    private final FaqRepository faqRepository;
    private final UserRepository userRepository;
    private final LoginUser loginUser;

    @GetMapping("/api/faqs")
    public ResponseEntity<?> listFaqs() {
        List<FaqSummaryDto> list = faqRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(f -> new FaqSummaryDto(f.getFaqId(), f.getQuestion(), f.getAnswer()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/api/faqs")
    public ResponseEntity<?> createFaq(@Valid @RequestBody FaqCreateRequest request,
                                       HttpServletRequest httpRequest) {

        String loginId;
        try {
            loginId = loginUser.loginId(httpRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }

        User admin = userRepository.findByLoginId(loginId).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        if (!"관리자".equals(admin.getUserName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("ok", false, "message", "관리자만 등록할 수 있습니다."));
        }

        Faq faq = new Faq();
        faq.setAdminId(admin.getUserId());
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());

        Faq saved = faqRepository.save(faq);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("ok", true, "faqId", saved.getFaqId()));
    }

    @PutMapping("/api/faqs/{id}")
    public ResponseEntity<?> updateFaq(@PathVariable("id") Long id,
                                       @Valid @RequestBody FaqCreateRequest request,
                                       HttpServletRequest httpRequest) {
        String loginId;
        try {
            loginId = loginUser.loginId(httpRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }

        User admin = userRepository.findByLoginId(loginId).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        if (!"관리자".equals(admin.getUserName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("ok", false, "message", "관리자만 수정할 수 있습니다."));
        }

        Faq faq = faqRepository.findById(id).orElse(null);
        if (faq == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "message", "존재하지 않는 FAQ 입니다."));
        }

        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faqRepository.save(faq);

        return ResponseEntity.ok(Map.of("ok", true, "faqId", faq.getFaqId()));
    }

    @DeleteMapping("/api/faqs/{id}")
    public ResponseEntity<?> deleteFaq(@PathVariable("id") Long id,
                                       HttpServletRequest httpRequest) {
        String loginId;
        try {
            loginId = loginUser.loginId(httpRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }

        User admin = userRepository.findByLoginId(loginId).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        if (!"관리자".equals(admin.getUserName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("ok", false, "message", "관리자만 삭제할 수 있습니다."));
        }

        Faq faq = faqRepository.findById(id).orElse(null);
        if (faq == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "message", "존재하지 않는 FAQ 입니다."));
        }

        faqRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
