package amgn.amu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentLogDto {
    private String type;
    private String status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}