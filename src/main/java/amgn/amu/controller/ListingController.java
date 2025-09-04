package amgn.amu.controller;

import amgn.amu.dto.AttrDto;
import amgn.amu.dto.ListingDto;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.entity.Listing;
import amgn.amu.repository.ListingRepository;
import amgn.amu.service.ListingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

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

    // ✅ 상품 등록 (파일 + JSON 데이터)
        @PostMapping("/write")
        @Transactional
        public ResponseEntity<Map<String, Object>> writeListing(
                @RequestParam("listingData") String listingDataJson,
                @RequestParam(value = "attrs", required = false) String attrsJson,
                @RequestParam("productPhotos") MultipartFile[] files,
                HttpSession session
        ) {
            try {
                // 로그인 확인
                LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
                if (loginUser == null) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("error", "로그인이 필요합니다.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
                }

                // JSON → DTO
                ListingDto listingDto = objectMapper.readValue(listingDataJson, ListingDto.class);
                listingDto.setSellerId(loginUser.getUserId());

                // Listing 저장
                Long listingId = listingService.saveListing(listingDto);

                // 파일 저장
                listingService.saveListingPhotos(listingId, files);

                // 속성(attrs) 저장
                if (attrsJson != null && !attrsJson.isEmpty()) {
                    List<AttrDto> attrs = objectMapper.readValue(attrsJson, new TypeReference<List<AttrDto>>() {});
                    listingService.saveListingAttrs(listingId, attrs);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("message", "업로드 성공");
                response.put("listingId", listingId);
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("상품 등록 실패", e);
                Map<String, Object> response = new HashMap<>();
                response.put("error", "업로드 실패");
                return ResponseEntity.status(500).body(response);
            }
        
    }


    @GetMapping("/all")
    public ResponseEntity<List<ListingDto>> getAllProducts() {
        try {
            return ResponseEntity.ok(listingService.getAllListings());
        } catch (Exception e) {
            log.error("모든 상품을 불러오는 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<ListingDto> getProductDetail(@PathVariable("listingId") long listingId) {
        try {
            ListingDto listingDto = listingService.getListingById(listingId);
            if (listingDto != null) return ResponseEntity.ok(listingDto);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("상품 정보를 불러오는 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<ListingDto>> getMyProducts(HttpSession session) {
        try {
            LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
            if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            return ResponseEntity.ok(listingService.getListingsBySellerId(loginUser.getUserId()));
        } catch (Exception e) {
            log.error("내 상품 목록을 불러오는 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}/related")
    public ResponseEntity<List<ListingDto>> getRelatedProducts(@PathVariable("id") Long id) {
        ListingDto product = listingService.getListingById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        List<ListingDto> relatedProducts = listingService.getListingsByCategoryExceptCurrent(
                product.getCategoryId().longValue(), id
        );

        return ResponseEntity.ok(relatedProducts);
    }


    @GetMapping("/search")
    public ResponseEntity<List<ListingDto>> searchByTitle(@RequestParam String title) {
        try {
            List<ListingDto> results = listingService.findByTitle(title);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("title 검색 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
 // 카테고리별 상품 조회
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ListingDto>> getProductsByCategory(@PathVariable("categoryId") Integer categoryId) {
       System.out.println(categoryId);
    	List<ListingDto> products = listingService.getListingsByCategory(categoryId);
    	System.out.println(products);
        return ResponseEntity.ok(products);
    }

}
