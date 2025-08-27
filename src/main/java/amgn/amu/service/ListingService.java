package amgn.amu.service;

import amgn.amu.entity.Listing;
import amgn.amu.domain.User;
import amgn.amu.dto.AttrDto;
import amgn.amu.dto.ListingDto;
import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingAttr;
import amgn.amu.entity.ListingPhoto;
import amgn.amu.repository.ListingAttrsRepository;
import amgn.amu.repository.ListingPhotosRepository;
import amgn.amu.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingAttrsRepository listingAttrsRepository;
    private final ListingPhotosRepository listingPhotosRepository;

    @Autowired
    public ListingService(
            ListingRepository listingRepository,
            ListingAttrsRepository listingAttrsRepository,
            ListingPhotosRepository listingPhotosRepository) {
        this.listingRepository = listingRepository;
        this.listingAttrsRepository = listingAttrsRepository;
        this.listingPhotosRepository = listingPhotosRepository;
    }

    @Transactional
    public Long saveListing(ListingDto dto) {
        Listing listing = new Listing();
        listing.setTitle(dto.getTitle());
        listing.setPrice(dto.getPrice());
        listing.setNegotiable(dto.getNegotiable());
        listing.setCategoryId(dto.getCategoryId());
        listing.setItemCondition(dto.getItemCondition());
        listing.setDescription(dto.getDescription());
        listing.setTradeType(dto.getTradeType());
        listing.setRegionId(dto.getRegionId());
        listing.setSafePayYn(dto.getSafePayYn());
        listing.setSellerId(dto.getSellerId());

        Listing savedListing = listingRepository.save(listing);
        return savedListing.getListingId();
    }

    @Transactional
    public void saveListingAttrs(Long listingId, List<AttrDto> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return;
        }
        List<ListingAttr> listingAttrs = attrs.stream()
                .map(dto -> {
                    ListingAttr attr = new ListingAttr();
                    attr.setListingId(listingId);
                    attr.setAttrKey(dto.getAttrKey());
                    attr.setAttrValue(dto.getAttrValue());
                    return attr;
                })
                .collect(Collectors.toList());

        listingAttrsRepository.saveAll(listingAttrs);
    }

    @Transactional
    public void saveListingPhotos(Long listingId, MultipartFile[] files) {
        // 1. listing 객체 조회
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // 2. 파일이 없으면 종료
        if (files == null || files.length == 0) {
            return;
        }

        // 3. 각 파일 처리
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];

            // 실제 S3 업로드 로직이 있으면 여기서 업로드 후 URL 가져오기
            String uploadedUrl = "https://your-s3-bucket/path/to/file_" + i + ".jpg";

            ListingPhoto photo = new ListingPhoto();
            photo.setListing(listing);  // Listing 객체 설정
            photo.setUrl(uploadedUrl);
            photo.setSortOrder(i);      // 순서 지정
            photo.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            listingPhotosRepository.save(photo);
        }
    }

    // 모든 상품 목록을 조회하는 메서드
    public List<ListingDto> getAllListings() {
        List<Listing> listings = listingRepository.findAll();
        return listings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ID로 특정 상품을 조회하는 메서드
    public ListingDto getListingById(long listingId) {
        Optional<Listing> listingOptional = listingRepository.findById(listingId);
        return listingOptional.map(this::convertToDto).orElse(null);
    }

    // Listing 엔티티를 ListingDto로 변환하는 헬퍼 메서드
    private ListingDto convertToDto(Listing listing) {
        ListingDto dto = new ListingDto();
        dto.setListingId(listing.getListingId());
        dto.setTitle(listing.getTitle());
        dto.setPrice(listing.getPrice());
        dto.setNegotiable(listing.getNegotiable());
        dto.setCategoryId(listing.getCategoryId());
        dto.setItemCondition(listing.getItemCondition());
        dto.setDescription(listing.getDescription());
        dto.setTradeType(listing.getTradeType());
        dto.setRegionId(listing.getRegionId());
        dto.setSafePayYn(listing.getSafePayYn());
        dto.setSellerId(listing.getSellerId());

        // 추가 필드 (예: photoUrl, sellerNickname)도 포함시켜야함
        // 예시: dto.setPhotoUrl(listing.getPhotos().get(0).getUrl());
        // 예시: dto.setSellerNickname(listing.getSeller().getNickname());
        return dto;
    }
    
    public static ListingDto toDto(Listing listing) {
        ListingDto dto = new ListingDto();
        dto.setListingId(listing.getListingId());
        dto.setSellerId(listing.getSellerId());
        dto.setTitle(listing.getTitle());
        dto.setPrice(listing.getPrice());
        dto.setNegotiable(listing.getNegotiable());
        dto.setCategoryId(listing.getCategoryId());
        dto.setItemCondition(listing.getItemCondition());
        dto.setDescription(listing.getDescription());
        dto.setTradeType(listing.getTradeType());
        dto.setRegionId(listing.getRegionId());
        dto.setSafePayYn(listing.getSafePayYn());
        return dto;
    }
}