package amgn.amu.dto;

import amgn.amu.domain.User;
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
    private String loginId;
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
    private LocalDateTime createdAt;

    public static LoginUserDto from(User user) {
        LoginUserDto dto = new LoginUserDto();
        dto.userId = user.getUserId();
        dto.loginId = user.getLoginId();
        dto.userName = user.getUserName();
        dto.email = user.getEmail();
        dto.nickName = user.getNickName();
        dto.phoneNumber = user.getPhoneNumber();
        dto.birthYear = user.getBirthYear();
        dto.birthMonth = user.getBirthMonth();
        dto.birthDay = user.getBirthDay();
        dto.createdAt = user.getCreatedAt();

        return dto;
    }
}
