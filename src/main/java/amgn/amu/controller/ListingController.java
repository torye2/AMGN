package amgn.amu.controller;

import amgn.amu.dto.AttrDto;
import amgn.amu.dto.ListingDto;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.service.ListingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
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
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> writeProduct(
            @RequestPart("listingData") String listingDataJson,
            @RequestPart("attrs") String attrsJson,
            @RequestParam("productPhotos") MultipartFile[] productPhotos,
            HttpSession session) {

        try {
            LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
            if (loginUser == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            ListingDto listingDto = objectMapper.readValue(listingDataJson, ListingDto.class);

            listingDto.setSellerId(loginUser.getUserId());

            List<AttrDto> attrs = Arrays.asList(objectMapper.readValue(attrsJson, AttrDto[].class));

            long listingId = listingService.saveListing(listingDto);

            listingService.saveListingAttrs(listingId, attrs);

            listingService.saveListingPhotos(listingId, productPhotos);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "상품이 성공적으로 등록되었습니다!");
            response.put("listingId", listingId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("상품 등록 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
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

    @GetMapping("/{listingId}")
    public ResponseEntity<ListingDto> getProductDetail(@PathVariable("listingId") long listingId) {
        try {
            ListingDto listingDto = listingService.getListingById(listingId);
            if (listingDto != null) {
                return ResponseEntity.ok(listingDto);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
            }
        } catch (Exception e) {
            log.error("상품 정보를 불러오는 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }
}