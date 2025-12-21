package com.back.domain.donation.controller;

import com.back.domain.donation.dto.request.DonationTossRequest;
import com.back.domain.donation.dto.response.DonationPaymentResponse;
import com.back.domain.donation.dto.response.DonationResponse;
import com.back.domain.donation.dto.response.DonorListResponse;
import com.back.domain.donation.service.DonationService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/donation")
@RequiredArgsConstructor
public class DonationController {
    private final DonationService donationService;

    @Operation(summary = "전체 후원 조회")
    @ApiResponse(
            responseCode = "200",
            description = "전체 후원 조회 성공",
            content = @Content(schema = @Schema(implementation = DonationResponse.class))
    )
    @GetMapping("/list")
    public ResponseEntity<RsData<List<DonationResponse>>> getAllDonations() {
        List<DonationResponse> donationList = donationService.getAllDonations();
        return ResponseEntity.ok().body(RsData.success("전체 후원 조회 성공", donationList));
    }


    @Operation(summary = "후원 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "후원 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = DonationResponse.class))
    )
    @GetMapping("/list/{id}")
    public ResponseEntity<RsData<DonationResponse>> getDonation(
            @PathVariable Long id
    ) {
        DonationResponse response = donationService.getDonation(id);
        return ResponseEntity.ok().body(RsData.success("후원 상세 조회 성공", response));
    }


    @Operation(summary = "상세보기별 후원 현황 조회")
    @ApiResponse(
            responseCode = "200",
            description = "상세보기별 후원 현황 조회 성공",
            content = @Content(schema = @Schema(implementation = DonationResponse.class))
    )
    @GetMapping("/list/{id}/donorList")
    public ResponseEntity<RsData<List<DonorListResponse>>> getDonationStatusById(
            @PathVariable Long id
    ) {
        List<DonorListResponse> donorList = donationService.getAllDonorList(id);
        return ResponseEntity.ok(RsData.success("상세보기별 후원 현황 조회 성공", donorList));
    }

    @Operation(summary = "TOSS 결제 ")
    @PostMapping("/toss/{donationId}/pay")
    public DonationPaymentResponse tossPayment(
            @PathVariable Long donationId, @RequestBody DonationTossRequest request
            ) {
        return donationService.donationTossPayment(donationId, request.getMemberId(), request);
    }
}
