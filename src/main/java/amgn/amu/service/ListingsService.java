package amgn.amu.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import amgn.amu.dto.PageDto;
import amgn.amu.dto.SearchDto;
import amgn.amu.mapper.ListingsMapper;
import amgn.amu.repository.CategoryRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ListingsService {

	private final ListingsMapper mapper;
	private final CategoryRepository categoryRepository;
	
	public Map<String, Object> getlist(SearchDto searchDto){
		System.out.println(searchDto);
		Map<String, Object> map = new HashMap<>();
		map.put("list", mapper.getLists(searchDto));
		int totalItems = mapper.getTotalCount(searchDto);
		System.out.println(totalItems);
		map.put("pageDto", new PageDto(searchDto, totalItems));
		map.put("subCategoryList", mapper.getSubCategoryList(searchDto));
		
		return map; 
	}
	

}
