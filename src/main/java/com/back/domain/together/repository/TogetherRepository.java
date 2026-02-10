package com.back.domain.together.repository;

import com.back.domain.together.entity.Together;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TogetherRepository extends JpaRepository<Together,Long> {

    List<Together> findByMember_Id(Long memberId);

    //최신순 카테고리x & 카테고리o
    @Query("""
    select t
    from Together t
    where (:mode is null or t.mode = :mode)
      and (:status is null or t.togetherStatus = :status)
          order by t.createdAt desc
    """)
    Page<Together> findLatestWithoutCategory(
            @Param("mode") TogetherMode mode,
            @Param("status") TogetherStatus status,
            Pageable pageable);
    @Query("""
    select t from Together t where (:categories is null or t.category in :categories)
        and (:mode is null or t.mode = :mode)
        and (:status is null or t.togetherStatus = :status)
            order by t.createdAt desc
    """)
    Page<Together> findLatestWithCategory(
            @Param("categories") List<TogetherCategory> categories,
            @Param("mode") TogetherMode mode,
            @Param("status") TogetherStatus status,
            Pageable pageable);

    //마감 임박순 카테고리x & 카테고리o
    @Query("""
    select t
    from Together t
    where (:mode is null or t.mode = :mode)
    and (:status is null or t.togetherStatus = :status)
        order by t.endDate asc
    """)
    Page<Together> findDeadlineWithoutCategory(
            @Param("mode") TogetherMode mode,
            @Param("status") TogetherStatus status,
            Pageable pageable);
    @Query("""
    select t
    from Together t
    where (:categories is null or t.category in :categories)
      and (:mode is null or t.mode = :mode)
      and (:status is null or t.togetherStatus = :status)
        order by t.endDate asc
""")
    Page<Together> findDeadlineWithCategory(
            @Param("categories") List<TogetherCategory> categories,
            @Param("mode") TogetherMode mode,
            @Param("status") TogetherStatus status,
            Pageable pageable);

    //인기순 (피드 개수 기준) 카테고리x & 카테고리o
    @Query("""
    select t
    from Together t
    left join Feed f on f.together = t
    where (:mode is null or t.mode = :mode)
      and (:status is null or t.togetherStatus = :status)
    group by t.id
    order by count(f.id) desc
    """)
    Page<Together> findPopularWithoutCategory(
            @Param("mode") TogetherMode mode,
            @Param("status") TogetherStatus status,
            Pageable pageable);

    @Query("""
    select t
    from Together t
    left join Feed f on f.together = t
    where (:categories is null or t.category in :categories)
      and (:mode is null or t.mode = :mode)
      and (:status is null or t.togetherStatus = :status)
    group by t.id
    order by count(f.id) desc
    """)
    Page<Together> findPopularWithCategory(
            @Param("categories") List<TogetherCategory> categories,
            @Param("mode") TogetherMode mode,
            @Param("status") TogetherStatus status,
            Pageable pageable);

    // 마이페이지용 참가 & 소유 투게더 조회
    @Query("""
    select distinct t
    from Together t
    left join t.participants p
    where t.member.id = :memberId
    or (p.member.id = :memberId and p.participantsStatus = com.back.domain.together.entity.ParticipantsStatus.PARTICIPATING)
        order by t.createdAt asc
    """)
    List<Together> findAllByMemberIdOrParticipants_MemberId(@Param("memberId") Long memberId);
}
