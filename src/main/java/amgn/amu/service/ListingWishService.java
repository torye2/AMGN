package amgn.amu.service;

import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingWish;
import amgn.amu.repository.ListingRepository;
import amgn.amu.repository.ListingWishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingWishService {

    private final ListingWishRepository wishRepository;
    private final ListingRepository listingRepository;

    @Transactional(readOnly = true)
    public boolean isWishedByUser(Long listingId, Long userId) {
        if (userId == null) return false;
        return wishRepository.existsByListingIdAndUserId(listingId, userId);
    }

    @Transactional(readOnly = true)
    public long getWishCount(Long listingId) {
        return wishRepository.countByListingId(listingId);
    }

    @Transactional
    public boolean toggle(Long listingId, Long userId) {
        boolean exists = wishRepository.existsByListingIdAndUserId(listingId, userId);
        if (exists) {
            wishRepository.deleteByListingIdAndUserId(listingId, userId);
        } else {
            ListingWish w = new ListingWish();
            w.setListingId(listingId);
            w.setUserId(userId);
            wishRepository.save(w);
        }
        syncCount(listingId);
        return !exists; // true면 '이제 찜됨'
    }

    @Transactional
    public void remove(Long listingId, Long userId) {
        wishRepository.deleteByListingIdAndUserId(listingId, userId);
        syncCount(listingId);
    }

    @Transactional
    public void syncCount(Long listingId) {
        long cnt = wishRepository.countByListingId(listingId);
        Listing listing = listingRepository.findById(listingId).orElseThrow();
        listing.setWishCount((int) cnt);
        listingRepository.save(listing);
        // 성능을 더 올리고 싶으면 ListingRepository에 JPQL update를 추가해도 좋아요.
        // ex) @Modifying @Query("update Listing l set l.wishCount=:cnt where l.listingId=:id")
    }

    @Transactional(readOnly = true)
    public List<Long> getListingIdsByUser(Long userId) {
        return wishRepository.findByUserId(userId).stream()
                .map(w -> w.getListingId())
                .collect(Collectors.toList());
    }

    //마이페이지 찜 개수 확인
    @Transactional(readOnly = true)
    public long getWishCountByUser(Long userId) {
        return wishRepository.countByUserId(userId);
    }
}
