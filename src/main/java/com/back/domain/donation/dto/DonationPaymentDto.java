package com.back.domain.donation.dto;

import com.back.domain.donation.entity.DonationPayments;

import java.time.LocalDateTime;

public class DonationPaymentDto {

    /* ===================Request====================== */
    /* ===================Response===================== */
    public record DonationPaymentResponse(
            Long donationId,
            Long amount,
            String paymentMethod
            // String status, //TODO: 최소한의 조건만 사용
            // LocalDateTime approvedAt
    ) {
        public static DonationPaymentResponse from(DonationPayments payments) {
            return new DonationPaymentResponse(
                    payments.getDonations().getId(),
                    payments.getAmount(),
                    payments.getPaymentMethod()
//                    payments.getStatus(), //TODO: 최소한의 조건만 사용
//                    payments.getApprovedAt()
            );
        }
    }
}
