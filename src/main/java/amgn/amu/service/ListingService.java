package amgn.amu.service;

import amgn.amu.dto.AttrDto;
import amgn.amu.dto.ListingDto;
import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingAttr;
import amgn.amu.entity.ListingPhoto;
import amgn.amu.repository.ListingAttrsRepository;
import amgn.amu.repository.ListingPhotosRepository;
import amgn.amu.repository.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ListingService {

	private final ListingRepository listingRepository;
	private final ListingAttrsRepository listingAttrsRepository;
	private final ListingPhotosRepository listingPhotosRepository;

	public ListingService(ListingRepository listingRepository,
						  ListingAttrsRepository listingAttrsRepository,
						  ListingPhotosRepository listingPhotosRepository) {
		this.listingRepository = listingRepository;
		this.listingAttrsRepository = listingAttrsRepository;
		this.listingPhotosRepository = listingPhotosRepository;
	}

	@Transactional
	public Long saveListing(ListingDto dto) {
		Listing listing = dto.toEntity();
		Listing saved = listingRepository.save(listing);
		return saved.getListingId();
	}

	@Transactional
	public void saveListingAttrs(Long listingId, List<AttrDto> attrs) {
		if (attrs == null || attrs.isEmpty()) return;

		Listing listing = listingRepository.findById(listingId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid listingId: " + listingId));

		List<ListingAttr> entities = attrs.stream().map(dto -> {
			ListingAttr attr = new ListingAttr();
			attr.setListing(listing);
			attr.setAttrKey(dto.getAttrKey());
			attr.setAttrValue(dto.getAttrValue());
			return attr;
		}).collect(Collectors.toList());

		listingAttrsRepository.saveAll(entities);
	}

	@Transactional
	public void saveListingPhotos(Long listingId, MultipartFile[] files) throws Exception {
		if (files == null || files.length == 0) return;

		Listing listing = listingRepository.findById(listingId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid listingId: " + listingId));

		for (MultipartFile file : files) {
			if (!file.isEmpty()) {
				String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
				Path savePath = Paths.get("C:/amu/uploads/listings/" + fileName); // 실제 저장 경로
				Files.createDirectories(savePath.getParent());
				file.transferTo(savePath.toFile());

				ListingPhoto photo = new ListingPhoto();
				photo.setListing(listing);
				photo.setUrl("/uploads/listings/" + fileName); // DB에 URL 저장
				listingPhotosRepository.save(photo);
			}
		}
	}





	@Transactional(readOnly = true)
	public List<ListingDto> findByTitle(String title) {
		List<Listing> entities = listingRepository.findByTitle(title);
		// 예: toDto(entity) 또는 ListingDto.from(entity)
		return entities.stream()
				.map(this::toDto)
				.toList();
	}

	private ListingDto toDto(Listing e) {
		ListingDto dto = new ListingDto();
		dto.setListingId(e.getListingId());
		dto.setSellerId(e.getSellerId());
		dto.setTitle(e.getTitle());
		dto.setPrice(e.getPrice());
		dto.setNegotiable(e.getNegotiable());
		dto.setCategoryId(e.getCategoryId());
		dto.setItemCondition(e.getItemCondition());
		dto.setDescription(e.getDescription());
		dto.setTradeType(e.getTradeType());
		dto.setRegionId(e.getRegionId());
		dto.setSafePayYn(e.getSafePayYn());
		// dto.setPhotoUrls(...);
		// dto.setSellerNickname(...);
		return dto;
	}



	public List<ListingDto> getListingsByCategoryExceptCurrent(Long categoryId, Long listingId) {
		List<Listing> listings = listingRepository.findByCategoryIdAndListingIdNot(categoryId, listingId);
		return listings.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	// 4 Listing → DTO 변환
	public ListingDto convertToDto(Listing listing) {
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
		dto.setSellerNickname(listing.getSeller().getNickName());

		if (listing.getPhotos() != null && !listing.getPhotos().isEmpty()) {
			List<String> urls = listing.getPhotos().stream()
					.map(ListingPhoto::getUrl)
					.collect(Collectors.toList());
			dto.setPhotoUrls(urls);
		}

		return dto;
	}

	// 5️⃣ 전체 상품 조회
	public List<ListingDto> getAllListings() {
		return listingRepository.findAll().stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	// 6️⃣ 판매자별 상품 조회
	public List<ListingDto> getListingsBySellerId(Long sellerId) {
		return listingRepository.findBySellerId(sellerId).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	// 7️⃣ ID 기준 상품 조회
	public ListingDto getListingById(long listingId) {
		return listingRepository.findById(listingId)
				.map(this::convertToDto)
				.orElse(null);
	}

	public List<ListingDto> getListingsByCategory(Integer categoryId) {
		// TODO Auto-generated method stub
		//return listingRepository.findByCategoryId(categoryId);
		return listingRepository.findByCategoryId(categoryId).stream()
				.map(this::convertToDto)
				.toList();
	}
}
