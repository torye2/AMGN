package amgn.amu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
    @NotNull Long orderId,
    @Min(1) @Max(5) int score,
    String rvComment
) {}
