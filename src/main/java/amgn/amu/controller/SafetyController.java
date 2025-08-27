package amgn.amu.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import amgn.amu.dto.BlockRequest;
import amgn.amu.dto.ReportCreateRequest;
import amgn.amu.dto.ReportDto;
import amgn.amu.service.SafetyService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

	private final SafetyService safetyService;
	
	// 신고하기
	@PostMapping("/reports")
	public ReportDto report(
			@RequestParam Long uid,
			@RequestBody ReportCreateRequest req) {
		return safetyService.report(uid, req);
	}
	
	// 차단하기
	@PostMapping("/blocks")
	public void block(
			@RequestParam Long uid,
			@RequestBody BlockRequest req) {
		safetyService.block(uid,  req.getBlockedId());
	}
	// 차단 해제
	public void unblock(
			@RequestParam Long uid,
			@PathVariable Long blockedId) {
		safetyService.unblock(uid,  blockedId);
	}
	
	// 차단 여부 확인
	public boolean isBlocked(
			@RequestParam Long uid,
			@PathVariable Long blockedId) {
		return safetyService.isBlocked(uid, blockedId);
	}
}
