package amgn.amu.dto;

import amgn.amu.entity.UserAddress;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    @NotBlank
    public Long addressId;
    public String addressType;
    public String recipientName;
    public String recipientPhone;
    public String postalCode;
    @NotBlank
    public String addressLine1;
    public String addressLine2;
    public String province;
    public String city;
    public String detailAddress;
    public Boolean isDefault;
    public String status;

    public static AddressDto from(UserAddress a, String province, String city) {
        AddressDto d = new AddressDto();
        d.addressId = a.getAddressId();
        d.addressType = a.getAddressType().name();
        d.recipientName = a.getRecipientName();
        d.recipientPhone = a.getRecipientPhone();
        d.postalCode = a.getPostalCode();
        d.addressLine1 = a.getAddressLine1();
        d.addressLine2 = a.getAddressLine2();
        d.detailAddress = a.getDetailAddress();
        d.isDefault = a.isDefault();
        d.status = a.getStatus().name();
        d.province = province;
        d.city = city;
        return d;
    }
}
