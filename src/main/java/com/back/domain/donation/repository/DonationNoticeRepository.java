package com.back.domain.donation.repository;

import com.back.domain.donation.entity.DonationNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationNoticeRepository extends JpaRepository<DonationNotice, Long> {

    DonationNotice findByDonationsId(Long id);
}
