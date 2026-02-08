package com.back.domain.donation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class DonationTossDto {

    /* ===================Request====================== */
    public record DonationTossRequest(
            String paymentKey,
            String orderId,
            Long amount,
            Long memberId
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
