package amgn.amu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class InquiryReplyDto {
    private Long replyId;
    private String content;
    private LocalDateTime createdAt;
}
