package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequest {
    @NotBlank
    public String addressType;    // HOME/WORK/SHIPPING/BILLING/OTHER
    public String recipientName;
    public String recipientPhone;
    public String postalCode;
    @NotBlank
    public String addressLine1;
    public String addressLine2;
    public String province;       // 프론트 합성 보조 필드 → 필요 시 regions 매핑
    public String city;
    public String detailAddress;  // 최종 상세(프론트 합성)
    public Boolean isDefault;
}
