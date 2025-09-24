package amgn.amu.service;

import amgn.amu.repository.ReportActionRepository;
import amgn.amu.repository.ReportEvidenceRepository;
import amgn.amu.repository.ReportRepository;
import amgn.amu.repository.UserSuspensionRepository;
import amgn.amu.service.util.UserDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportEvidenceRepository evidenceRepository;
    private final ReportRepository reportRepository;
    private final ReportActionRepository actionRepository;
    private final UserSuspensionRepository suspensionRepository;
    //private final UserDirectory userDirectory;
}
