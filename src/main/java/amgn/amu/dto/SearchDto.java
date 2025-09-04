package amgn.amu.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchDto {
	
	private int pageNo = 1; // 요청 페이지 번호
	private int amount = 10; // 페이지당 게시물 수
	
	// 검색에 필요한 정보
	private String searchField ="" ; // 검색필드
	private String searchWord =""; // 검색어
	
	private int categoryId;
	public SearchDto(String pageNo, String amount) {
		try {
			if(pageNo != null)
				this.pageNo = Integer.parseInt(pageNo);
			if(amount != null)
				this.amount = Integer.parseInt(amount);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error ==== 페이지번호, 페이지당 게시물수를 세팅중 예외가 발생 하였습니다. [형변환 오류]");
		}
	}
	
	public SearchDto(String pageNo, String amount, String searchField, String searchWord) {
		// 생성자 호출
		this(pageNo, amount);
		this.searchField = searchField;
		this.searchWord = searchWord;
	}
}
