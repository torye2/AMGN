package amgn.amu.dto;

public record UserProfileDto(
   String id,
   String userName,
   String email,
   String nickName,
   String gender,
   String phoneNumber,
   Integer birthYear,
   Integer birthMonth,
   Integer birthDay,
   String province,
   String city,
   String detailAddress
) {}
