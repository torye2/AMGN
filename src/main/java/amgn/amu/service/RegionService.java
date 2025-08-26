package amgn.amu.service;

import java.util.List;

import org.springframework.stereotype.Service;

import amgn.amu.dto.RegionDto;
import amgn.amu.repository.RegionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegionService {

	private final RegionRepository regionRepository;
	
	public List<RegionDto>getByParentId(Long parentId){
		 return regionRepository.findByParentIdOrderByNameAsc(parentId)
	                .stream()
	                .map(r -> new RegionDto(r.getId(), r.getName(), r.getParentId(), r.getPath(), r.getLeveNo()))
	                .toList();
	}
}
