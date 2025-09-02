package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequest(
        String id,
        String newPassword,
        String email,
        String nickName,
        String phoneNumber,
        Integer birthYear,
        Integer birthMonth,
        Integer birthDay,
        String province,
        String city,
        String detailAddress
) {}
