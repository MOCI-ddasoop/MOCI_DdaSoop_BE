package com.back.domain.feed.repository;

import com.back.domain.feed.entity.MemberTagStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MemberTagStatistics Repository
 * 회원별 자주 사용하는 태그 통계 조회
 */
public interface MemberTagStatisticsRepository extends JpaRepository<MemberTagStatistics, Long> {

    /**
     * 특정 회원의 태그 통계 조회
     * 
     * @param memberId 회원 ID
     * @return 태그 통계 (없으면 Optional.empty())
     */
    Optional<MemberTagStatistics> findByMemberId(Long memberId);

    /**
     * 특정 회원의 태그 통계 존재 여부 확인
     * 
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    boolean existsByMemberId(Long memberId);

    /**
     * 오래된 통계 조회 (7일 이상)
     * 배치 작업으로 재계산할 통계 목록
     * 
     * @param before 이 시간 이전에 업데이트된 통계
     * @return 오래된 통계 목록
     */
    @Query("SELECT mts FROM MemberTagStatistics mts " +
           "WHERE mts.updatedAt < :before")
    List<MemberTagStatistics> findStaleStatistics(@Param("before") LocalDateTime before);

    /**
     * 통계가 없는 회원 조회
     * 초기 통계 생성용
     * 
     * @return 통계가 없는 회원 ID 목록
     */
    @Query("SELECT m.id FROM Member m " +
           "WHERE m.id NOT IN (SELECT mts.member.id FROM MemberTagStatistics mts)")
    List<Long> findMembersWithoutStatistics();
}
