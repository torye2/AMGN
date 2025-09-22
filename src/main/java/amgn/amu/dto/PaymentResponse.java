package amgn.amu.dto;

public record PaymentResponse(
        boolean success,
        String message,
        String merchantUid,
        long amount,
        String orderName
) {}
