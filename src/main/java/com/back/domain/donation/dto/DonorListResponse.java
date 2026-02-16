package com.back.domain.donation.dto;

import java.time.LocalDateTime;

public record DonorListResponse(
        Long donationPaymentId,
        Long memberId,
        String memberName,
        Long amount,
        String paymentMethod,
        LocalDateTime createdAt
) {
}

