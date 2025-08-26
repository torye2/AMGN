package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserDto {
    @NotBlank
    private String id;
    @NotBlank
    private String userName;
    @NotBlank
    private String email;
    @NotBlank
    private String nickName;
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private Integer birthYear;
    @NotBlank
    private Integer birthMonth;
    @NotBlank
    private Integer birthDay;
    @NotBlank
    private String province;
    @NotBlank
    private String city;

    private String detailAddress;
}
