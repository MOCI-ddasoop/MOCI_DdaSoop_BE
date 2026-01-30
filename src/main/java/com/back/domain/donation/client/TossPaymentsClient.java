package com.back.domain.donation.client;

import com.back.domain.donation.dto.request.DonationTossRequest;
import com.back.domain.donation.dto.response.DonationPaymentResponse;
import com.back.domain.donation.dto.response.DonationTossResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TossPaymentsClient {

    @Value("${TOSS_SECRET_KEY}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public DonationTossResponse confirm(DonationTossRequest request) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(secretKey, "");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<DonationTossRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<DonationTossResponse> response =
                restTemplate.postForEntity(
                        "https://api.tosspayments.com/v1/payments/confirm",
                        entity,
                        DonationTossResponse.class
                );

        return response.getBody();
    }
}