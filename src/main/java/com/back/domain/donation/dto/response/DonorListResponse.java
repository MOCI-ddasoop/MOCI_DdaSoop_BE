package com.back.domain.donation.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DonorListResponse {

    private Long donationPaymentId;
    private Long memberId;
    private String memberName;
    private Long amount;
    private String paymentMethod;
    private LocalDateTime createdAt;

    // 🔥 JPQL용 생성자 (필수)
    public DonorListResponse(
            Long donationPaymentId,
            Long memberId,
            String memberName,
            Long amount,
            String paymentMethod,
            LocalDateTime createdAt
    ) {
        this.donationPaymentId = donationPaymentId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
    }
}
