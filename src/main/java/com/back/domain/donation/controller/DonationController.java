package com.back.domain.donation.controller;

import com.back.domain.donation.dto.DonationDto;
import com.back.domain.donation.dto.DonationPaymentDto;
import com.back.domain.donation.dto.DonationTossDto;
import com.back.domain.donation.dto.DonorListResponse;
import com.back.domain.donation.service.DonationService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/donation")
@RequiredArgsConstructor
public class DonationController {
    private final DonationService donationService;

    /**
     * SecurityContext에서 현재 로그인한 회원 ID 추출
     * @return 현재 로그인한 회원 ID
     */
    private Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        return (Long) authentication.getPrincipal();
    }

    @Operation(summary = "전체 후원 조회")
    @ApiResponse(
            responseCode = "200",
            description = "전체 후원 조회 성공",
            content = @Content(schema = @Schema(implementation = DonationDto.ListResponse.class))
    )
    @GetMapping("/list")
    public ResponseEntity<RsData<List<DonationDto.ListResponse>>> getAllDonations() {
        List<DonationDto.ListResponse> donationList = donationService.getAllDonations();
        return ResponseEntity.ok().body(RsData.success("전체 후원 조회 성공", donationList));
    }


    @Operation(summary = "후원 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "후원 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = DonationDto.DetailResponse.class))
    )
    @GetMapping("/list/{id}")
    public ResponseEntity<RsData<DonationDto.DetailResponse>> getDonation(
            @PathVariable Long id
    ) {
        DonationDto.DetailResponse response = donationService.getDonation(id);
        return ResponseEntity.ok().body(RsData.success("후원 상세 조회 성공", response));
    }

    @Operation(summary = "후원하기 리스트 설명 조회")
    @ApiResponse(
            responseCode = "200",
            description = "후원하기 리스트 설명 조회 성공",
            content = @Content(schema = @Schema(implementation = DonationDto.DescriptionResponse.class))
    )
    @GetMapping("/list/{id}/description")
    public ResponseEntity<RsData<DonationDto.DescriptionResponse>> getDonationDescription(
            @PathVariable Long id
    ) {
        DonationDto.DescriptionResponse response = donationService.getDonationDescription(id);
        return ResponseEntity.ok().body(RsData.success("후원하기 리스트 설명 조회 성공", response));
    }


    @Operation(summary = "상세보기별 후원 현황 조회")
    @ApiResponse(
            responseCode = "200",
            description = "상세보기별 후원 현황 조회 성공",
            content = @Content(schema = @Schema(implementation = DonorListResponse.class))
    )
    @GetMapping("/list/{id}/donorList")
    public ResponseEntity<RsData<List<DonorListResponse>>> getDonationStatusById(
            @PathVariable Long id
    ) {
        List<DonorListResponse> donorList = donationService.getAllDonorList(id);
        return ResponseEntity.ok(RsData.success("상세보기별 후원 현황 조회 성공", donorList));
    }

    @Operation(summary = "TOSS 결제")
    @ApiResponse(
            responseCode = "200",
            description = "TOSS 결제 성공",
            content = @Content(schema = @Schema(implementation = DonationPaymentDto.DonationPaymentResponse.class))
    )
    @PostMapping("/toss/{donationId}/pay")
    public ResponseEntity<RsData<DonationPaymentDto.DonationPaymentResponse>> tossPayment(
            @PathVariable Long donationId, @Valid @RequestBody DonationTossDto.DonationTossRequest request
    ) {
        DonationPaymentDto.DonationPaymentResponse response = donationService.donationTossPayment(donationId, request.memberId(), request);
        return ResponseEntity.ok(RsData.success("TOSS 결제 성공", response));
    }
}
