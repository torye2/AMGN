package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDto {

	private int pageNo;
	private int amount;
	private int totalItems ;
	private int totalPages ;
	
	private int startNo;
	private int endNo;
	private boolean isPrev ;
	private boolean isNext ;
	
	
	
	public PageDto(SearchDto searchDto, int totalItems) {
		this.pageNo = searchDto.getPageNo();
		this.amount = searchDto.getAmount();
		this.totalItems = totalItems;
		
		this.totalPages = (int) Math.ceil((double) totalItems / amount);
		if(totalItems <= amount) {
			this.totalPages = 1 ;
		}
		int blockSize = 5;
		this.startNo = ((pageNo - 1)/blockSize) * blockSize + 1;
		this.endNo = Math.min(startNo + blockSize - 1, totalPages);
		
		this.isPrev = startNo > 1;
		this.isNext = endNo < totalPages;
		
		
	}
}
