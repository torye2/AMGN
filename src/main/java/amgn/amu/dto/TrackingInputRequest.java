package amgn.amu.dto;

import jakarta.validation.constraints.NotBlank;

//운송장(선택 기능: 배송형만)
public record TrackingInputRequest(
	    @NotBlank String carrier,
	    @NotBlank String trackingNo
	) {}
