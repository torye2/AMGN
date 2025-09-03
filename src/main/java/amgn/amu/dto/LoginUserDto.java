package amgn.amu.dto;

import amgn.amu.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserDto {
    private Long userId;
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
    @NotBlank
    private LocalDateTime createdAt;

    public static LoginUserDto from(User user) {
        LoginUserDto dto = new LoginUserDto();
        dto.userId = user.getUserId();
        dto.id = user.getId();
        dto.userName = user.getUserName();
        dto.email = user.getEmail();
        dto.nickName = user.getNickName();
        dto.phoneNumber = user.getPhoneNumber();
        dto.birthYear = user.getBirthYear();
        dto.birthMonth = user.getBirthMonth();
        dto.birthDay = user.getBirthDay();
        dto.province = user.getProvince();
        dto.city = user.getCity();
        dto.detailAddress = user.getDetailAddress();
        dto.createdAt = user.getCreatedAt();

        return dto;
    }
}
