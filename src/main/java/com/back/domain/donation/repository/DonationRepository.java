package com.back.domain.donation.repository;

import com.back.domain.donation.entity.DonationCategory;
import com.back.domain.donation.entity.Donations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donations,Long> {
    // 최신순 카테고리x & 카테고리o
    @Query("""
        select d
        from Donations d
        order by d.createdAt desc
    """)
    Page<Donations> findLatestWithoutCategory(Pageable pageable);

    @Query("""
        select d
        from Donations d
        where (:categories is null or d.donationCategory in :categories)
        order by d.createdAt desc
    """)
    Page<Donations> findLatestWithCategory(
            @Param("categories") List<DonationCategory> categories,
            Pageable pageable);

    // 마감 임박순 카테고리x & 카테고리o
    @Query("""
        select d
        from Donations d
        order by d.endDate asc
    """)
    Page<Donations> findDeadlineWithoutCategory(Pageable pageable);

    @Query("""
        select d
        from Donations d
        where (:categories is null or d.donationCategory in :categories)
        order by d.endDate asc
    """)
    Page<Donations> findDeadlineWithCategory(
            @Param("categories") List<DonationCategory> categories,
            Pageable pageable);

    // 인기순 (후원 수 기준) 카테고리x & 카테고리o
    @Query("""
        select d
        from Donations d
        left join DonationPayments dp on dp.donations = d
        group by d.id
        order by count(dp.id) desc
    """)
    Page<Donations> findPopularWithoutCategory(Pageable pageable);

    @Query("""
        select d
        from Donations d
        left join DonationPayments dp on dp.donations = d
        where (:categories is null or d.donationCategory in :categories)
        group by d.id
        order by count(dp.id) desc
    """)
    Page<Donations> findPopularWithCategory(
            @Param("categories") List<DonationCategory> categories,
            Pageable pageable);

    // "나"가 참여한 후원하기 & 개설한 후원하기 조회
    @Query("""
        select distinct d
        from Donations d
        left join d.donationParticipants dp
        where d.member.id = :memberId
        or (dp.member.id = :memberId and dp.participantsStatus = com.back.domain.donation.entity.DonationParticipantStatus.PARTICIPATING)
            order by d.createdAt asc
    """)
    List<Donations> findByAllMemberIdOrParticipants_MemberId(@Param("memberId") Long memberId);
}
