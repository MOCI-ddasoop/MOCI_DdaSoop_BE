package com.back.domain.donation.repository;

import com.back.domain.donation.entity.DonationParticipants;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationParticipantsRepository extends JpaRepository<DonationParticipants, Integer> {
}
