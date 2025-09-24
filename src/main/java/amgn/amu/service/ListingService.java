package amgn.amu.service;

import amgn.amu.dto.AttrDto;
import amgn.amu.dto.ListingDto;
import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingAttr;
import amgn.amu.entity.ListingPhoto;
import amgn.amu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ListingService {

	private final ListingRepository listingRepository;
	private final ListingAttrsRepository listingAttrsRepository;
	private final ListingPhotosRepository listingPhotosRepository;
	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;

	public ListingService(ListingRepository listingRepository,
						  ListingAttrsRepository listingAttrsRepository,
						  ListingPhotosRepository listingPhotosRepository,
						  CategoryRepository categoryRepository,
						  RegionRepository regionRepository) {
		this.listingRepository = listingRepository;
		this.listingAttrsRepository = listingAttrsRepository;
		this.listingPhotosRepository = listingPhotosRepository;
		this.categoryRepository = categoryRepository;
		this.regionRepository = regionRepository;
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



	public List<ListingDto> getListingsByCategoryExceptCurrent(Long categoryId, Long listingId) {
		List<Listing> listings = listingRepository.findByCategoryIdAndListingIdNot(categoryId, listingId);
		return listings.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	// Listing → DTO 변환
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
		dto.setStatus(listing.getStatus());

		String regionName = null;
		try {
			Integer ridInt = listing.getRegionId();
			Long rid = (ridInt == null) ? null : ridInt.longValue();
			if (rid != null) {
				regionName = regionRepository.getNameById(rid);
			}
			log.info("after region lookup: regionName={}", regionName);
		} catch (Exception e) {
			log.warn("regionName resolve failed. regionId={}, err={}", listing.getRegionId(), e.toString());
		}
		dto.setRegionName(regionName);



		if (listing.getPhotos() != null && !listing.getPhotos().isEmpty()) {
			List<String> urls = listing.getPhotos().stream()
					.map(ListingPhoto::getUrl)
					.toList();
			dto.setPhotoUrls(urls);

			List<ListingDto.PhotoDto> photoDtos = listing.getPhotos().stream()
					.map(p -> {
						ListingDto.PhotoDto pd = new ListingDto.PhotoDto();
						pd.setPhotoId(p.getPhotoId());
						pd.setUrl(p.getUrl());
						return pd;
					}).toList();
			dto.setPhotos(photoDtos);
		}

		return dto;
	}

	// 전체 상품 조회
	public List<ListingDto> getAllListings() {
		return listingRepository.findAll().stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	// 판매자별 상품 조회
	public List<ListingDto> getListingsBySellerId(Long sellerId) {
		return listingRepository.findBySellerId(sellerId).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	public List<ListingDto> getListingsBySellerIdAndStatus(Long sellerId, String status) {
		return listingRepository.findBySellerIdAndStatus(sellerId, status).stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}


	//  ID 기준 상품 조회
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

	@Transactional(readOnly = true)
	public Listing getListingEntity(Long id) {
		return listingRepository.findById(id).orElse(null);
	}

	@Transactional
	public void updateListing(Listing entity, ListingDto dto) {
		if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
		if (dto.getPrice() != null) entity.setPrice(dto.getPrice());
		if (dto.getNegotiable() != null) entity.setNegotiable(dto.getNegotiable());
		if (dto.getCategoryId() != null) entity.setCategoryId(dto.getCategoryId());
		if (dto.getItemCondition() != null) entity.setItemCondition(dto.getItemCondition());
		if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
		if (dto.getTradeType() != null) entity.setTradeType(dto.getTradeType());
		if (dto.getRegionId() != null) entity.setRegionId(dto.getRegionId());
		if (dto.getSafePayYn() != null) entity.setSafePayYn(dto.getSafePayYn());
		listingRepository.save(entity);
	}

	@Transactional
	public void replaceListingAttrs(Long listingId, List<AttrDto> attrs) {
		listingAttrsRepository.deleteByListing_ListingId(listingId);
		Listing listingRef = listingRepository.getReferenceById(listingId);
		if (attrs == null) return;
		for (AttrDto a : attrs) {
			if ((a.getAttrKey() == null || a.getAttrKey().isBlank())
					&& (a.getAttrValue() == null || a.getAttrValue().isBlank())) continue;
			ListingAttr la = new ListingAttr();
			la.setListing(listingRef);
			la.setAttrKey(a.getAttrKey());
			la.setAttrValue(a.getAttrValue());
			listingAttrsRepository.save(la);
		}
	}

	@Transactional
	public void deleteListingPhotos(Long listingId, List<Long> photoIds) {
		if (photoIds == null || photoIds.isEmpty()) return;
		// 실제 파일 삭제가 필요하면 여기서 스토리지도 함께 삭제 처리
		listingPhotosRepository.deleteByListing_ListingIdAndPhotoIdIn(listingId, photoIds);
	}

	@Transactional
	public void deleteListing(Long listingId) {
		// 1) 첨부/속성 등 자식 먼저 정리 (연관관계/제약에 따라 순서 중요)
		// 물리 파일 삭제가 필요하면 여기서 photo url로 스토리지도 지워줘야 함.
		listingAttrsRepository.deleteByListing_ListingId(listingId);
		listingPhotosRepository.deleteByListing_ListingId(listingId);

		// 2) 본문 삭제
		listingRepository.deleteById(listingId);
	}

	/** 지역별 상품 (비페이징) */
	public List<ListingDto> getListingsByRegion(Long regionId) {
		List<Listing> rows = listingRepository.findByRegionIdOrderByListingIdDesc(regionId);
		return rows.stream().map(ListingDto::from).toList(); // ListingDto::from 이 있다면 사용
	}

	/** 지역별 상품 (페이지네이션) - regionId 없으면 전체 */
	public Page<ListingDto> findByRegionPaged(Long regionId, Pageable pageable) {
		Page<Listing> page = (regionId == null)
				? listingRepository.findAll(pageable)
				: listingRepository.findByRegionId(regionId, pageable);
		return page.map(ListingDto::from);
	}

	@Transactional(readOnly = true)
	public Page<ListingDto> search(Long categoryId, Long regionId, Pageable pageable) {
		List<Long> catIds = null;
		if (categoryId != null) {
			String catPath = categoryRepository.getPathById(categoryId);
			// path가 없거나 비어있으면 name으로 대체
			if (catPath == null || catPath.isBlank()) {
				String catName = categoryRepository.getNameById(categoryId);
				if (catName != null && !catName.isBlank()) {
					catPath = catName;
				}
			}
			if (catPath != null && !catPath.isBlank()) {
				catIds = new ArrayList<>(categoryRepository.findIdsByPathPrefix(catPath));
				if (catIds.isEmpty()) {
					catIds = new ArrayList<>();
				}
				// 자기 자신도 포함 보장
				if (!catIds.contains(categoryId)) catIds.add(categoryId);
			} else {
				catIds = List.of(categoryId);
			}
		}

		List<Long> regIds = null;
		if (regionId != null) {
			String regPath = regionRepository.getPathById(regionId);
			if (regPath == null || regPath.isBlank()) {
				String regName = regionRepository.getNameById(regionId);
				if (regName != null && !regName.isBlank()) {
					regPath = regName;
				}
			}
			if (regPath != null && !regPath.isBlank()) {
				regIds = new ArrayList<>(regionRepository.findIdsByPathPrefix(regPath));
				if (regIds.isEmpty()) {
					regIds = new ArrayList<>();
				}
				if (!regIds.contains(regionId)) regIds.add(regionId);
			} else {
				regIds = List.of(regionId);
			}
		}

		log.info("search() expanded: catIds={}, regIds={}", catIds, regIds);

		Page<Listing> page;
		if (catIds != null && regIds != null) {
			page = listingRepository.findByCategoryIdInAndRegionIdIn(catIds, regIds, pageable);
		} else if (catIds != null) {
			page = listingRepository.findByCategoryIdIn(catIds, pageable);
		} else if (regIds != null) {
			page = listingRepository.findByRegionIdIn(regIds, pageable);
		} else {
			page = listingRepository.findAll(pageable);
		}

		return page.map(this::convertToDto);
	}

	@Transactional
	public void softDeleteById(Long listingId) {
		listingRepository.deleteById(listingId); // @SQLDelete가 UPDATE로 처리
	}

}
