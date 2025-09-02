package amgn.amu.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignupDto {
    private String id;
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+{}\\[\\]:;\"'<>,.?/~`-]).{8,}$")
    private String passwordHash;
    private String userName;
    private String nickName;
    private String email;
    private String phoneNumber;
    private String gender;
    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;
    private String province;
    private String city;
    private String detailAddress;
}
