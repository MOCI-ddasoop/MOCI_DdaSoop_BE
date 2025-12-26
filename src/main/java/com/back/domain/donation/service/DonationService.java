package com.back.domain.donation.service;

import com.back.domain.donation.client.TossPaymentsClient;
import com.back.domain.donation.dto.request.DonationTossRequest;
import com.back.domain.donation.dto.response.DonationPaymentResponse;
import com.back.domain.donation.dto.response.DonationResponse;
import com.back.domain.donation.dto.response.DonationTossResponse;
import com.back.domain.donation.dto.response.DonorListResponse;
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

    public List<DonationResponse> getAllDonations() {
        return donationRepository.findAll().stream()
                .map(DonationResponse::from)
                .toList();
    }

    public DonationResponse getDonation(Long id) {
        var donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "번 후원 없음"));
        return DonationResponse.from(donation);
    }

    public List<DonorListResponse> getAllDonorList(Long id){

        donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "번 후원 없음"));
        // 후원에 대한 기부자 리스트 조회
        return donationRepository.findAllDonorList(id);
    }

    //Toss 결제 승인 및 후원 결제 내역 저장
    @Transactional
    public DonationPaymentResponse donationTossPayment(
            Long donationId, Long memberId, DonationTossRequest request
    ) {
        // 후원 대상 조회
        Donations donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("후원 대상 없음"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        // 토스 결제 승인
        DonationTossResponse tossResponse =
                tossPaymentsClient.confirm(request);

        TossPayments tossPayments = TossPayments.builder()
                .paymentKey(tossResponse.getPaymentKey())
                .orderId(tossResponse.getOrderId())
                .amount(tossResponse.getTotalAmount())
                .status(TossPaymentStatus.DONE)
                .approvedAt(tossResponse.getApprovedAt())
                .member(member)
                .build();

        tossPaymentRepository.save(tossPayments);

        // 결제 내역 저장
        DonationPayments payment = DonationPayments.builder()
                .donations(donation)
                .member(member)
                .amount(tossResponse.getTotalAmount())
                .paymentMethod("TOSS")
//                .approvedAt(tossResponse.getApprovedAt())
//                .tossPayments(tossPayments)
                .build();

        donationPaymentsRepository.save(payment);

        if (TossPaymentStatus.DONE.name().equals(tossResponse.getStatus())) {
            donation.increaseAmount(tossResponse.getTotalAmount().intValue());
        }

        // 프론트로 응답
        return DonationPaymentResponse.from(payment);
    }
}
