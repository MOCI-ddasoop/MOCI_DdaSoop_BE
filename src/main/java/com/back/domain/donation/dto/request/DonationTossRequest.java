package com.back.domain.donation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DonationTossRequest {

    @NotBlank(message = "결제는 필수 입니다.")
    private String paymentKey;

    @NotBlank(message = "주문 id는 필수 입니다.")
    private String orderId;

    @NotNull(message = "결제 금액은 필수 입니다.")
    private Long amount;

    private Long memberId;
}
