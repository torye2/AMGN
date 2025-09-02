package amgn.amu.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import amgn.amu.dto.PageDto;
import amgn.amu.dto.SearchDto;
import amgn.amu.mapper.ListingsMapper;

@Service
public class ListingsService {

	private final ListingsMapper mapper;
	
	public ListingsService(ListingsMapper mapper) {
		this.mapper = mapper;
	}
	public Map<String, Object> getlist(SearchDto searchDto){
		System.out.println(searchDto);
		Map<String, Object> map = new HashMap<>();
		map.put("list", mapper.getLists(searchDto));
		int totalItems = mapper.getTotalCount(searchDto);
		map.put("pageDto", new PageDto(searchDto, totalItems));

		return map; 
	}
	

}
