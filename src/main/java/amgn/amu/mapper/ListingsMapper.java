package amgn.amu.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import amgn.amu.dto.ListingsDto;
import amgn.amu.dto.SearchDto;

@Mapper
public interface ListingsMapper {
	@Select("""
			select title, description, price, seller_id, category_id
			from listings
			ORDER BY created_at DESC 
			OFFSET (#{pageNo} - 1) * #{amount} ROWS 
			FETCH NEXT #{amount} ROWS ONLY
			""")
	List<ListingsDto> getLists(SearchDto search);
	
	int getTotalCount(SearchDto searchDto);
}
