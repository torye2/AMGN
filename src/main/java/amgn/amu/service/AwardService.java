package amgn.amu.service;

import amgn.amu.repository.AwardQueryRepository;
import amgn.amu.repository.TopSellerRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AwardService {
    private final AwardQueryRepository repo;

    public List<TopSellerRow> getTopSellersByUnits(int limit) {
        if (limit <= 0 || limit > 50) limit = 3;
        LocalDate ymStart = LocalDate.now().withDayOfMonth(1);
        String ym = ymStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return repo.findTopSellers(ym, "units", PageRequest.of(0, limit));
    }
}
