package amgn.amu.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import amgn.amu.dto.ListingsDto;
import amgn.amu.dto.SearchDto;
import amgn.amu.service.ListingsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/product")
public class ListingsController {

	private final ListingsService listingsService;
	
	public ListingsController(ListingsService listingsService) {
		this.listingsService = listingsService;
	}
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getList(SearchDto searchDto){
				System.out.println("리스트 조회 컨트롤러 호출");
				System.out.println("pageNo=" + searchDto.getPageNo());
			    System.out.println("amount=" + searchDto.getAmount());
			    System.out.println("categoryIds=" + searchDto.getCategoryIds());
		try {
			Map<String, Object> map = listingsService.getlist(searchDto);
			System.out.println(map);
			return ResponseEntity.ok(map);
		} catch (Exception e) {
			log.error("상품 정보를 불러오는 중 오류 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
