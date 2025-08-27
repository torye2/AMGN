package amgn.amu.service;

import amgn.amu.dto.ReportCreateRequest;
import amgn.amu.dto.ReportDto;

public interface SafetyService {

	ReportDto report(Long reporterId, ReportCreateRequest req);
	void block(Long blockerId, Long blockedId);
	void unblock(Long blockerId, Long blockedId);
	boolean isBlocked(Long blockerId, Long blockedId);
}
