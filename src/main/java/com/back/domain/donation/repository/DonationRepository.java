package com.back.domain.donation.repository;

import com.back.domain.donation.dto.response.DonorListResponse;
import com.back.domain.donation.entity.Donations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donations,Long> {

    @Query("""
    select new com.back.domain.donation.dto.response.DonorListResponse(
        dp.id, m.id, m.name, dp.amount, dp.paymentMethod, dp.createdAt
    )
    from DonationPayments dp
    join dp.member m
    where dp.donations.id = :donationId
    order by dp.createdAt desc
""")
    List<DonorListResponse> findAllDonorList(@Param("donationId") Long donationId);

}
