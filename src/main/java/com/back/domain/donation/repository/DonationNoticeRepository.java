package com.back.domain.donation.repository;

import com.back.domain.donation.entity.DonationNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationNoticeRepository extends JpaRepository<DonationNotice, Long> {
}
