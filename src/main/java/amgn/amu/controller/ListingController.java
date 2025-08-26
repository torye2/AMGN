package amgn.amu.controller;

import amgn.amu.dto.AttrDto;
import amgn.amu.dto.ListingDto;
import amgn.amu.service.ListingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/product")
public class ListingController {

    private final ListingService listingService;
    private final ObjectMapper objectMapper;

    public ListingController(ListingService listingService, ObjectMapper objectMapper) {
        this.listingService = listingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/write")
    @Transactional(rollbackFor = Exception.class) // 모든 예외에 대해 롤백하도록 수정
    public ResponseEntity<Map<String, String>> writeProduct(
            @RequestPart("listingData") String listingDataJson,
            @RequestPart("attrs") String attrsJson,
            @RequestParam("productPhotos") MultipartFile[] productPhotos) {

        try {
            // JSON 문자열을 DTO 객체로 변환
            ListingDto listingDto = objectMapper.readValue(listingDataJson, ListingDto.class);
            List<AttrDto> attrs = Arrays.asList(objectMapper.readValue(attrsJson, AttrDto[].class));

            // DTO를 사용하여 비즈니스 로직 처리
            // 1. listings 테이블에 먼저 저장하고, 생성된 listingId를 반환받습니다.
            long listingId = listingService.saveListing(listingDto);

            // 2. listing_attrs 테이블에 저장
            listingService.saveListingAttrs(listingId, attrs);

            // 3. listing_photos 테이블에 저장 (파일 업로드 처리 후)
            listingService.saveListingPhotos(listingId, productPhotos);

            // 모든 작업이 성공하면 성공 응답을 보냅니다.
            Map<String, String> response = new HashMap<>();
            response.put("message", "상품이 성공적으로 등록되었습니다!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생", e);
            // 예외 발생 시 오류 응답을 보냅니다.
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "상품 등록 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/all")
    public ResponseEntity<List<ListingDto>> getAllProducts() {
        try {
            List<ListingDto> allListings = listingService.getAllListings();
            return ResponseEntity.ok(allListings);
        } catch (Exception e) {
            log.error("모든 상품을 불러오는 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}