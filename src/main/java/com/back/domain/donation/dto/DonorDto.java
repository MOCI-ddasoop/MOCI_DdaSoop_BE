package com.back.domain.donation.dto;

import com.back.domain.donation.entity.DonationPayments;
import com.back.domain.member.entity.Member;

import java.time.LocalDateTime;

public class DonorDto {
    /* ===================Request====================== */
    /* ===================Response===================== */
    public record ListResponse(
            Long donationPaymentId,
            Long memberId,
            String memberName,
            Long amount,
            String paymentMethod,
            LocalDateTime createdAt
    ) {
        public static ListResponse from(DonationPayments payments) {
            Member member = payments.getMember();

            return new ListResponse(
                    payments.getId(),
                    member.getId(),
                    member.getName(),
                    payments.getAmount(),
                    payments.getPaymentMethod(),
                    payments.getCreatedAt()
            );
        }
    }

}

