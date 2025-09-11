package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FaqSummaryDto {
    private Long faqId;
    private String question;
    private String answer;
}
