package amgn.amu.service;

import amgn.amu.entity.Listing; // domain 패키지 import
import amgn.amu.entity.ListingAttr; // domain 패키지 import
import amgn.amu.entity.ListingPhoto; // domain 패키지 import
import amgn.amu.dto.AttrDto;
import amgn.amu.dto.ListingDto;
import amgn.amu.entity.Listing;
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
        Listing listing = dto.toEntity(); // DTO to Entity 변환
        Listing savedListing = listingRepository.save(listing);
        return savedListing.getListingId();
    }

    @Transactional
    public void saveListingAttrs(Long listingId, List<AttrDto> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return;
        }

        // listingId를 사용하여 Listing 엔티티 객체를 가져옵니다.
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid listingId: " + listingId));

        List<ListingAttr> listingAttrs = attrs.stream()
                .map(dto -> {
                    ListingAttr attr = new ListingAttr();
                    // setListingId 대신 setListing()을 호출합니다.
                    attr.setListing(listing);
                    attr.setAttrKey(dto.getAttrKey());
                    attr.setAttrValue(dto.getAttrValue());
                    return attr;
                })
                .collect(Collectors.toList());

        listingAttrsRepository.saveAll(listingAttrs);
    }

    @Transactional
    public void saveListingPhotos(Long listingId, MultipartFile[] files) {
        // listingId를 사용하여 Listing 엔티티 객체를 가져옵니다.
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid listingId: " + listingId));

        // 실제로는 파일을 S3에 업로드하고 URL을 받아와야 합니다.
        // 여기서는 예시로 더미 URL을 사용합니다.
        for (MultipartFile file : files) {
            ListingPhoto photo = new ListingPhoto();
            // setListingId 대신 setListing()을 호출합니다.
            photo.setListing(listing);
            photo.setUrl("https://your-s3-bucket/path/to/uploaded/file.jpg");
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

    public List<ListingDto> getListingsBySellerId(Long sellerId) {
        List<Listing> listings = listingRepository.findBySellerId(sellerId);
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
        if (listing.getPhotos() != null && !listing.getPhotos().isEmpty()) {
            dto.setPhotoUrl(listing.getPhotos().get(0).getUrl());
        }
        // 추가 필드 (예: photoUrl, sellerNickname)도 포함시켜야함
        // 예시: dto.setPhotoUrl(listing.getPhotos().get(0).getUrl());
        // 예시: dto.setSellerNickname(listing.getSeller().getNickname());
        return dto;
    }
}