package amgn.amu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FindIdRequest {
    @NotBlank
    private String userName;
    @NotNull @Min(1950)
    private Integer birthYear;
    @NotNull @Min(1) @Max(12)
    private Integer birthMonth;
    @NotNull @Min(1) @Max(31)
    private Integer birthDay;
    @NotBlank
    private String phoneNumber;
}
