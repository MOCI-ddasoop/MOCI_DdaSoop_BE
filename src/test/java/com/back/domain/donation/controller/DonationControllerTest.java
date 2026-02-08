package com.back.domain.donation.controller;

import com.back.domain.donation.service.DonationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DonationController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
//                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
        }
)
@ActiveProfiles("test")
class DonationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DonationService donationService;

//    @Test
//    @DisplayName("1. 전체 후원 목록 조회")
//    void getDonationList_success() throws Exception {
//
//        DonationResponse response = DonationResponse.builder()
//                .title("겨울 이불 후원")
//                .description("추운 겨울 이불 지원")
//                .goalAmount(100000L)
//                .currentAmount(5000L)
//                .startDate("2025-01-01T00:00")
//                .endDate("2025-01-31T23:59")
//                .status("ONGOING")
//                .build();
//
//        Mockito.when(donationService.getAllDonations())
//                .thenReturn(List.of(response));
//
//        mockMvc.perform(get("/api/v1/donation/list"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data[0].title").value("겨울 이불 후원"))
//                .andExpect(jsonPath("$.data[0].goalAmount").value(100000L))
//                .andExpect(jsonPath("$.data[0].currentAmount").value(5000L))
//                .andExpect(jsonPath("$.data[0].status").value("ONGOING"));
//    }
//
//    @Test
//    @DisplayName("2. 후원 상세 조회")
//    void getDonationDetail_success() throws Exception {
//
//        DonationResponse response = DonationResponse.builder()
//                .title("겨울 이불 후원")
//                .description("추운 겨울 이불 지원")
//                .goalAmount(100000L)
//                .currentAmount(8000L)
//                .startDate("2025-01-01T00:00")
//                .endDate("2025-01-31T23:59")
//                .status("ONGOING")
//                .build();
//
//        Mockito.when(donationService.getDonation(1L))
//                .thenReturn(response);
//
//        mockMvc.perform(get("/api/v1/donation/list/{donationId}", 1L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.title").value("겨울 이불 후원"))
//                .andExpect(jsonPath("$.data.currentAmount").value(8000L));
//    }
//
//    @Test
//    @DisplayName("3. 후원자 목록 조회")
//    void getDonorList_success() throws Exception {
//
//        DonorListResponse donor = new DonorListResponse(
//                1L,
//                1L,
//                "테스트 후원자",
//                1000L,
//                "TOSS",
//                LocalDateTime.of(2025, 1, 1, 0, 0)
//        );
//
//        Mockito.when(donationService.getAllDonorList(1L))
//                .thenReturn(List.of(donor));
//
//        mockMvc.perform(get("/api/v1/donation/list/{donationId}/donorList", 1L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data[0].memberId").value(1L))
//                .andExpect(jsonPath("$.data[0].amount").value(1000L));
//    }
//
//    @Test
//    @DisplayName("4. 토스 결제 승인 성공")
//    void tossPayment_success() throws Exception {
//
//        String requestJson = """
//                {
//                  \"paymentKey\": \"test_payment_key\",
//                  \"orderId\": \"order-test-001\",
//                  \"amount\": 1000,
//                  \"memberId\": 1
//                }
//                """;
//
//        DonationPaymentResponse response = DonationPaymentResponse.builder()
//                .donationId(1L)
//                .amount(1000L)
//                .status("DONE")
//                .build();
//
//        Mockito.when(
//                donationService.donationTossPayment(
//                        Mockito.eq(1L),
//                        Mockito.eq(1L),
//                        Mockito.any(DonationTossRequest.class)
//                )
//        ).thenReturn(response);
//
//        mockMvc.perform(
//                        post("/api/v1/donation/toss/{donationId}/pay", 1L)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(requestJson)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.amount").value(1000L))
//                .andExpect(jsonPath("$.data.status").value("DONE"));
//    }
}