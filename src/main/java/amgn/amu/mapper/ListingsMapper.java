package amgn.amu.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import amgn.amu.dto.ListingsDto;
import amgn.amu.dto.SearchDto;

@Mapper
public interface ListingsMapper {
	
	List<ListingsDto> getLists(SearchDto search);
	

	int getTotalCount(SearchDto searchDto);
}
