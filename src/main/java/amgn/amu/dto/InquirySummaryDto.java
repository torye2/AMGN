package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InquirySummaryDto {
    private Long inquiryId;
    private String title;
    private String content;
}
