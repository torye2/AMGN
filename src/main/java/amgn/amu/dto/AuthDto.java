package amgn.amu.dto;

import jakarta.validation.constraints.*;

public class AuthDto {

    public record CheckIdRequest(
            @NotBlank @Size(min=4, max=20) String id
    ) {}

    public record CheckIdResponse(boolean available) {}

    public record SendCodeRequest(
            @NotBlank String channel,
            @NotBlank String destination
    ) {}

    public record VerifyCodeRequest(
            @NotBlank String channel,
            @NotBlank String destination,
            @NotBlank String code
    ) {}

    public record SignupRequest(
            @NotBlank @Size(min=4, max=20) String id,
            @NotBlank @Size(min=8, max=64) String password,
            @NotBlank @Email String email,
            @NotBlank String nickName,
            @NotBlank @Pattern(regexp = "^[MF]$") String gender,
            @NotBlank String phoneNumber,
            @NotNull Integer birthYear,
            @NotNull Integer birthMonth,
            @NotNull Integer birthDay,
            @NotBlank String province,
            @NotBlank String city,
            @NotBlank String detailAddress
    ) {}

    public record AuthUserDto(Long userId, String id, String nickName) {}
}
