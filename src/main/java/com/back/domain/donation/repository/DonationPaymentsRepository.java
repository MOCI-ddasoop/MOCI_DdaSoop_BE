package com.back.domain.donation.repository;

import com.back.domain.donation.entity.DonationPayments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DonationPaymentsRepository
        extends JpaRepository<DonationPayments, Long> {
    @Query("""
    select  dp
    from DonationPayments dp
    join fetch dp.member m
    where dp.donations.id = :donationId
    order by dp.createdAt desc
""")
    List<DonationPayments> findAllDonorList(@Param("donationId") Long donationId);

    // 마이 페이지 - 내가 후원한 내역 조회
    @Query("""
    select dp
    from DonationPayments dp
    join fetch dp.donations d
    where dp.member.id = :memberId
    order by dp.createdAt desc
""")
    List<DonationPayments> findAllMyDonationPaymentByMember_Id(Long memberId);
}