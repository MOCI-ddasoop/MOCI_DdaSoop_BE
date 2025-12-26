package com.back.domain.donation.repository;

import com.back.domain.donation.entity.DonationPayments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationPaymentsRepository
        extends JpaRepository<DonationPayments, Long> {
}