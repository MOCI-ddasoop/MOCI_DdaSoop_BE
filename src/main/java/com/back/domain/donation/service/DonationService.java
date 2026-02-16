package com.back.domain.donation.service;

import com.back.domain.donation.client.TossPaymentsClient;
import com.back.domain.donation.dto.DonationDto;
import com.back.domain.donation.dto.DonationPaymentDto;
import com.back.domain.donation.dto.DonationTossDto;
import com.back.domain.donation.dto.DonorListResponse;
import com.back.domain.donation.entity.DonationPayments;
import com.back.domain.donation.entity.Donations;
import com.back.domain.donation.entity.TossPaymentStatus;
import com.back.domain.donation.entity.TossPayments;
import com.back.domain.donation.repository.DonationPaymentsRepository;
import com.back.domain.donation.repository.DonationRepository;
import com.back.domain.donation.repository.TossPaymentRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {
    private final DonationRepository donationRepository;
    private final DonationPaymentsRepository donationPaymentsRepository;
    private final TossPaymentRepository tossPaymentRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final MemberRepository memberRepository;

    public List<DonationDto.ListResponse> getAllDonations() {
        return donationRepository.findAll().stream()
                .map(DonationDto.ListResponse::from)
                .toList();
    }

    public DonationDto.DetailResponse getDonation(Long id) {
        var donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "번 후원하기 없음"));
        return DonationDto.DetailResponse.from(donation);
    }

    public DonationDto.DescriptionResponse getDonationDescription(Long id) {
        Donations donations = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "번 후원하기 설명 없음"));
        String description = donations.getDescription();
        return new DonationDto.DescriptionResponse(
                description == null ? "" : description
        );
    }

    public List<DonorListResponse> getAllDonorList(Long id){

        donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "번 후원 없음"));
        // 후원에 대한 기부자 리스트 조회
        return donationRepository.findAllDonorList(id);
    }

    //Toss 결제 승인 및 후원 결제 내역 저장
    @Transactional
    public DonationPaymentDto.DonationPaymentResponse donationTossPayment(
            Long donationId, Long memberId, DonationTossDto.DonationTossRequest request
    ) {
        // 후원 대상 조회
        Donations donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("후원페이지(donationId)를 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원(memberId)을 찾을 수 없습니다."));

        // 토스 결제 승인
        DonationTossDto.DonationTossResponse tossResponse =
                tossPaymentsClient.confirm(request);

        TossPayments tossPayments = TossPayments.builder()
                .paymentKey(tossResponse.paymentKey())
                .orderId(tossResponse.orderId())
                .amount(tossResponse.totalAmount())
                .status(TossPaymentStatus.DONE)
                .approvedAt(tossResponse.approvedAt())
                .member(member)
                .build();

        tossPaymentRepository.save(tossPayments);

        // 결제 내역 저장
        DonationPayments payment = DonationPayments.builder()
                .donations(donation)
                .member(member)
                .amount(tossResponse.totalAmount())
                .paymentMethod("TOSS")
//                .approvedAt(tossResponse.getApprovedAt()) //TODO: 일단 최소한으로 조건 사용
//                .tossPayments(tossPayments)
                .build();

        donationPaymentsRepository.save(payment);

        if (TossPaymentStatus.DONE.name().equals(tossResponse.status())) {
            donation.increaseAmount(tossResponse.totalAmount());
        }

        // 프론트로 응답
        return DonationPaymentDto.DonationPaymentResponse.from(payment);
    }
}
