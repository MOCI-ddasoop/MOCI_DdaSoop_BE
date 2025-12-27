package com.back.domain.report.repository;

import com.back.domain.report.entity.Report;
import com.back.domain.report.entity.ReportStatus;
import com.back.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Report Repository
 * 신고 데이터 조회 및 관리
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

    // ========== 중복 신고 확인 ==========

    /**
     * 중복 신고 방지 - 이미 신고했는지 확인
     */
    boolean existsByReporterIdAndTargetTypeAndTargetId(
        Long reporterId,
        ReportTargetType targetType,
        Long targetId
    );

    // ========== 신고 횟수 조회 ==========

    /**
     * 특정 대상의 신고 횟수
     * - 자동 조치 판단 (10회 이상이면 자동 비공개되도록 처리 할 수 있는 형식 등에 사용 가능)
     */
    Long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    /**
     * 특정 회원이 받은 신고 횟수
     * - 회원 제재 판단
     * - 신고 이력 조회
     */
    Long countByReportedMemberId(Long reportedMemberId);

    /**
     * 특정 회원이 한 신고 횟수
     * - 악의적 신고 방지 (하루 10회 이상 신고 제한)
     */
    Long countByReporterId(Long reporterId);

    // ========== 신고 목록 조회 ==========

    /**
     * 특정 대상의 신고 목록 (관리자용)
     * - 피드 상세 페이지에서 "이 게시물의 신고 내역" 조회
     */
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
        ReportTargetType targetType,
        Long targetId
    );

    /**
     * 상태별 신고 목록 조회 (관리자용, 페이징)
     * - 관리자 페이지: "대기 중인 신고" 목록
     */
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    /**
     * 특정 타입 + 상태별 신고 목록 조회 (관리자용, 페이징)
     * - 관리자 페이지: "피드 신고 중 대기 중인 것만" 조회
     */
    Page<Report> findByTargetTypeAndStatusOrderByCreatedAtDesc(
        ReportTargetType targetType,
        ReportStatus status,
        Pageable pageable
    );

    /**
     * 특정 회원이 받은 신고 목록 (관리자용)
     * - 회원 관리: "홍길동님이 받은 신고 내역"
     */
    Page<Report> findByReportedMemberIdOrderByCreatedAtDesc(
        Long reportedMemberId,
        Pageable pageable
    );

    /**
     * 특정 회원이 한 신고 목록
     * - 마이페이지: "내가 신고한 내역"
     */
    Page<Report> findByReporterIdOrderByCreatedAtDesc(
        Long reporterId,
        Pageable pageable
    );

    // ========== 통계 조회 ==========

    /**
     * 처리 대기 중인 신고 개수
     * - 관리자 대시보드: "처리 대기 중: 23건" 표시
     */
    Long countByStatus(ReportStatus status);

    /**
     * 특정 타입의 처리 대기 중인 신고 개수
     * - 관리자 대시보드: "피드 신고 대기: 10건, 댓글 신고 대기: 5건"
     */
    Long countByTargetTypeAndStatus(ReportTargetType targetType, ReportStatus status);

    // ========== 복합 조회 (Custom Query) ==========

    /**
     * 신고 횟수가 많은 콘텐츠 조회 (관리자용)
     * - "10회 이상 신고된 피드" 조회
     */
    @Query("SELECT r.targetId FROM Report r " +
           "WHERE r.targetType = :targetType " +
           "GROUP BY r.targetId " +
           "HAVING COUNT(r) >= :minCount " +
           "ORDER BY COUNT(r) DESC")
    List<Long> findFrequentlyReportedTargets(
        @Param("targetType") ReportTargetType targetType,
        @Param("minCount") Long minCount,
        Pageable pageable
    );

    /**
     * 신고를 많이 받은 회원 조회 (관리자용)
     */
    @Query("SELECT r.reportedMemberId FROM Report r " +
           "WHERE r.reportedMemberId IS NOT NULL " +
           "GROUP BY r.reportedMemberId " +
           "HAVING COUNT(r) >= :minCount " +
           "ORDER BY COUNT(r) DESC")
    List<Long> findFrequentlyReportedMembers(
        @Param("minCount") Long minCount,
        Pageable pageable
    );
}
