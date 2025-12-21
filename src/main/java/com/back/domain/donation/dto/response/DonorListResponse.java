package com.back.domain.donation.dto.response;

import java.time.LocalDateTime;

public record DonorListResponse(
        Long paymentId,
        Long memberId,
        String memberName,
        int amount,
        String paymentMethod,
        LocalDateTime donatedAt
) {}
