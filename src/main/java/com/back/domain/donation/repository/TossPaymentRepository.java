package com.back.domain.donation.repository;

import com.back.domain.donation.entity.TossPayments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TossPaymentRepository extends JpaRepository<TossPayments, Long> {

    Optional<TossPayments> findByPaymentKey(String paymentKey);
}
