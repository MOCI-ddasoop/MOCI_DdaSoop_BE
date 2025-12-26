package com.back.domain.donation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DonationTossRequest {

    private String paymentKey;
    private String orderId;
    private Long amount;

    private Long memberId;
}
