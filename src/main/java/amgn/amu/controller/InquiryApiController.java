package amgn.amu.controller;

import amgn.amu.common.LoginUser;
import amgn.amu.domain.User;
import amgn.amu.dto.InquiryCreateRequest;
import amgn.amu.dto.InquiryReplyCreateRequest;
import amgn.amu.dto.InquirySummaryDto;
import amgn.amu.entity.Inquiry;
import amgn.amu.entity.InquiryReply;
import amgn.amu.repository.InquiryRepository;
import amgn.amu.repository.InquiryReplyRepository;
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
public class InquiryApiController {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final LoginUser loginUser;
    private final InquiryReplyRepository inquiryReplyRepository;

    @PostMapping("/api/inquiries")
    public ResponseEntity<Map<String, Object>> createInquiry(
            @Valid @RequestBody InquiryCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = null;
        try {
            String loginId = loginUser.loginId(httpRequest);
            User user = userRepository.findByLoginId(loginId).orElse(null);
            if (user != null) userId = user.getUserId();
        } catch (IllegalStateException ignored) {
            // 비로그인 허용: userId = null 로 저장
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setUserId(userId);
        inquiry.setTitle(request.getTitle());
        inquiry.setContent(request.getContent());
        inquiry.setStatus(Inquiry.Status.PENDING);

        Inquiry saved = inquiryRepository.save(inquiry);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("inquiryId", saved.getInquiryId()));
    }

    @GetMapping("/api/inquiries/my")
    public ResponseEntity<?> getMyInquiries(HttpServletRequest httpRequest) {
        String loginId;
        try {
            loginId = loginUser.loginId(httpRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<Inquiry> list = inquiryRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
        List<InquirySummaryDto> result = list.stream()
                .map(i -> new InquirySummaryDto(
                        i.getInquiryId(),
                        i.getTitle(),
                        i.getContent(),
                        i.getStatus() != null ? i.getStatus().name() : null
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/inquiries")
    public ResponseEntity<?> getAllInquiriesForAdmin(HttpServletRequest httpRequest) {
        String loginId;
        try {
            loginId = loginUser.loginId(httpRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        // username 이 '관리자' 인 사용자만 허용
        if (!"관리자".equals(user.getUserName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "접근 권한이 없습니다."));
        }
        List<Inquiry> list = inquiryRepository.findAllByOrderByCreatedAtDesc();
        List<InquirySummaryDto> result = list.stream()
                .map(i -> new InquirySummaryDto(
                        i.getInquiryId(),
                        i.getTitle(),
                        i.getContent(),
                        i.getStatus() != null ? i.getStatus().name() : null
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/inquiries/{inquiryId}/replies")
    public ResponseEntity<?> replyToInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryReplyCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        // 로그인 사용자 확인
        String loginId;
        try {
            loginId = loginUser.loginId(httpRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        User admin = userRepository.findByLoginId(loginId).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        // 관리자만 허용
        if (!"관리자".equals(admin.getUserName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "관리자만 답변할 수 있습니다."));
        }
        // 문의 존재 확인
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElse(null);
        if (inquiry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "문의가 존재하지 않습니다."));
        }

        // 답변 저장
        InquiryReply reply = new InquiryReply();
        reply.setInquiryId(inquiryId);
        reply.setAdminId(admin.getUserId());
        reply.setContent(request.getContent());
        InquiryReply saved = inquiryReplyRepository.save(reply);

        // 문의 상태를 ANSWERED 로 변경 (선택)
        inquiry.setStatus(Inquiry.Status.ANSWERED);
        inquiryRepository.save(inquiry);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "replyId", saved.getReplyId(),
                "inquiryId", inquiryId
        ));
    }

    @GetMapping("/api/inquiries/{inquiryId}/replies")
    public ResponseEntity<?> getRepliesForInquiry(
            @PathVariable Long inquiryId,
            HttpServletRequest httpRequest
    ) {
        // 로그인 사용자 확인
        String loginId;
        try {
            loginId = loginUser.loginId(httpRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }

        // 권한 체크: 관리자이거나, 해당 문의의 작성자여야 함
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElse(null);
        if (inquiry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "문의가 존재하지 않습니다."));
        }
        boolean isAdmin = "관리자".equals(user.getUserName());
        if (!isAdmin && (inquiry.getUserId() == null || !inquiry.getUserId().equals(user.getUserId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "접근 권한이 없습니다."));
        }

        var replies = inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(inquiryId)
                .stream()
                .map(r -> new amgn.amu.dto.InquiryReplyDto(r.getReplyId(), r.getContent(), r.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(replies);
    }
}
