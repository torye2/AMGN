package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryReplyCreateRequest {

    @NotBlank
    private String content;
}
