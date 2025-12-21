package com.back.domain.donation.service;

import com.back.domain.donation.dto.response.DonationResponse;
import com.back.domain.donation.dto.response.DonorListResponse;
import com.back.domain.donation.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {
    private final DonationRepository donationRepository;

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
}
