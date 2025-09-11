package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FaqCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String question;

    @NotBlank
    private String answer;
}
