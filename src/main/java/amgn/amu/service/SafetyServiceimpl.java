package amgn.amu.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import amgn.amu.dto.ReportDto;
import amgn.amu.entity.Block;
import amgn.amu.entity.Block.PK;
import amgn.amu.entity.Report;
import amgn.amu.repository.BlockRepository;
import amgn.amu.repository.ReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SafetyServiceimpl implements SafetyService {

	private final BlockRepository blockRepository;
	private final ReportRepository reportRepository;
	
	public ReportDto report(Long reporterId, ReportCreateRequest req) {
		Report report = new Report();
		report.setReporterId(reporterId);
		report.setTargetType(req.getTargetType());
		report.setTargetId(req.getTargetId());
		report.setReason(req.getReason());
		report.setStatus("PENDING");
		report.setCreatedAt((int) Instant.now().getEpochSecond());
		
		Report saved = reportRepository.save(report);
		
		return new ReportDto(
				saved.getReportId(),
				saved.getReporterId(),
				saved.getTargetType(),
				saved.getTargetId(),
				saved.getReason(),
				saved.getStatus(),
				saved.getCreatedAt()
				);
	}
	@Override
	@Transactional
	public void block(Long blockerId, Long blockedId) {
		Block.PK pk = new Block.PK(blockerId, blockedId);
		if(!blockRepository.existsById(pk)) {
			Block block = new Block(pk, (int)Instant.now().getEpochSecond());
			blockRepository.save(block);
		}
	}
	@Override
	@Transactional
	public void unblock(Long blockerId, Long blockedId) {
		Block.PK pk = new Block.PK(blockerId, blockedId);
		if(blockRepository.existsById(pk)) {
			blockRepository.deleteById(pk);
		}
	}
	public boolean isBlocked(Long blockerId, Long blockedId) {
		Block.PK pk = new PK(blockerId, blockedId);
		return blockRepository.existsById(pk);
	}
}
