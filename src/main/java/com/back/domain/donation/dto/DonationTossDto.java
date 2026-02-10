package com.back.domain.donation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class DonationTossDto {

    /* ===================Request====================== */
    public record DonationTossRequest(
            @NotBlank String paymentKey,
            @NotBlank String orderId,
            @NotNull Long amount,
            @NotNull Long memberId
    ) {
    }

    /* ===================Response===================== */
    public record DonationTossResponse(
            String paymentKey,
            String orderId,
            Long totalAmount,
            String status,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            LocalDateTime approvedAt
    ) {
    }
}
