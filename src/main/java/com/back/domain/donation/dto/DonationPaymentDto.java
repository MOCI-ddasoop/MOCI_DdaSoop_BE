package com.back.domain.donation.dto;

import com.back.domain.donation.entity.DonationPayments;

import java.time.LocalDateTime;
import java.util.List;

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

    public record DonationPaymentListResponse(
            Long id,
            Long donationId,
            Long memberId,
            String title,
            String thumbnailImage,
            Long amount,
            String paymentMethod,
            LocalDateTime createdAt
    ) {
        public static DonationPaymentListResponse from(DonationPayments payments) {
            return new DonationPaymentListResponse(
                    payments.getId(),
                    payments.getDonations().getId(),
                    payments.getMember().getId(),
                    payments.getDonations().getTitle(),
                    getFirstImageUrlOrNull(payments.getDonations().getImageUrls()),
                    payments.getAmount(),
                    payments.getPaymentMethod(),
                    payments.getCreatedAt()
            );
        }
    }

    private static String getFirstImageUrlOrNull(List<String> imageUrls) {
        return (imageUrls == null || imageUrls.isEmpty()) ? null : imageUrls.getFirst();
    }

    public record RecentDonationPaymentListResponse(
            Long id,
            Long donationId,
            Long memberId,
            String memberName,
            String title,
            String thumbnailImage,
            Long amount,
            String paymentMethod,
            LocalDateTime createdAt
    ) {
        public static RecentDonationPaymentListResponse from(DonationPayments payments) {
            return new RecentDonationPaymentListResponse(
                    payments.getId(),
                    payments.getDonations().getId(),
                    payments.getMember().getId(),
                    payments.getMember().getName(),
                    payments.getDonations().getTitle(),
                    getFirstImageUrlOrNull(payments.getDonations().getImageUrls()),
                    payments.getAmount(),
                    payments.getPaymentMethod(),
                    payments.getCreatedAt()
            );
        }
    }
}
