package com.back.domain.donation.dto.response;

import com.back.domain.donation.entity.DonationPayments;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationPaymentResponse {

    private Long donationId;
    private Long amount;
    private String paymentMethod;
    private String status;
    private LocalDateTime approvedAt;

    public static DonationPaymentResponse from(DonationPayments payment) {
        return DonationPaymentResponse.builder()
                .donationId(payment.getDonations().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
//                .status(payment.getStatus())
//                .approvedAt(payment.getApprovedAt())
                .build();
    }
}
