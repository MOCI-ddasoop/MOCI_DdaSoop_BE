package com.back.domain.feed.repository;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Feed Repository (Spring Data JPA 기반)
 */
public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {

    // ========== 기본 조회 (Spring Data JPA) ==========
    
    /**
     * ID로 삭제되지 않은 피드 단건 조회
     */
    Optional<Feed> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 관리자용 피드 목록 조회 (필터 + 페이징, 최신순)
     */
    @Query(
        value = """
            SELECT f FROM Feed f
            WHERE f.deletedAt IS NULL
              AND (:visibility IS NULL OR f.visibility = :visibility)
              AND (:authorId IS NULL OR f.member.id = :authorId)
              AND (:reportedOnly = false OR EXISTS (
                    SELECT 1 FROM Report r
                    WHERE r.targetType = com.back.domain.report.entity.ReportTargetType.FEED
                      AND r.targetId = f.id
              ))
            ORDER BY f.createdAt DESC
            """,
        countQuery = """
            SELECT COUNT(f) FROM Feed f
            WHERE f.deletedAt IS NULL
              AND (:visibility IS NULL OR f.visibility = :visibility)
              AND (:authorId IS NULL OR f.member.id = :authorId)
              AND (:reportedOnly = false OR EXISTS (
                    SELECT 1 FROM Report r
                    WHERE r.targetType = com.back.domain.report.entity.ReportTargetType.FEED
                      AND r.targetId = f.id
              ))
            """
    )
    Page<Feed> findAdminFeeds(
            @Param("visibility") FeedVisibility visibility,
            @Param("authorId") Long authorId,
            @Param("reportedOnly") boolean reportedOnly,
            Pageable pageable
    );
    
    // ========== Top N 조회 (인기 피드) ==========
    
    /**
     * 댓글 많은 피드 Top N (visibility 필터 적용)
     *
     * @param currentMemberId 현재 로그인한 회원 ID (null이면 비로그인 → PUBLIC/FOLLOWERS만)
     * @param limit           조회할 개수
     */
    @Query("""
        SELECT f FROM Feed f
        WHERE f.deletedAt IS NULL
          AND (
            f.visibility IN (
                com.back.domain.feed.entity.FeedVisibility.PUBLIC,
                com.back.domain.feed.entity.FeedVisibility.FOLLOWERS
            )
            OR (f.visibility = com.back.domain.feed.entity.FeedVisibility.PRIVATE
                AND (:currentMemberId IS NOT NULL AND f.member.id = :currentMemberId))
            OR (f.visibility = com.back.domain.feed.entity.FeedVisibility.MEMBERS
                AND (:currentMemberId IS NOT NULL
                    AND (f.member.id = :currentMemberId
                         OR EXISTS (
                             SELECT 1 FROM Participants p
                             WHERE p.together.id = f.together.id
                               AND p.member.id = :currentMemberId
                               AND p.participantsStatus = com.back.domain.together.entity.ParticipantsStatus.PARTICIPATING
                         ))))
          )
        ORDER BY f.commentCount DESC, f.createdAt DESC
        LIMIT :limit
        """)
    List<Feed> findTopByCommentCountWithVisibility(
            @Param("currentMemberId") Long currentMemberId,
            @Param("limit") int limit
    );

    /**
     * 북마크 많은 피드 Top N (visibility 필터 적용)
     *
     * @param currentMemberId 현재 로그인한 회원 ID (null이면 비로그인 → PUBLIC/FOLLOWERS만)
     * @param limit           조회할 개수
     */
    @Query("""
        SELECT f FROM Feed f
        WHERE f.deletedAt IS NULL
          AND (
            f.visibility IN (
                com.back.domain.feed.entity.FeedVisibility.PUBLIC,
                com.back.domain.feed.entity.FeedVisibility.FOLLOWERS
            )
            OR (f.visibility = com.back.domain.feed.entity.FeedVisibility.PRIVATE
                AND (:currentMemberId IS NOT NULL AND f.member.id = :currentMemberId))
            OR (f.visibility = com.back.domain.feed.entity.FeedVisibility.MEMBERS
                AND (:currentMemberId IS NOT NULL
                    AND (f.member.id = :currentMemberId
                         OR EXISTS (
                             SELECT 1 FROM Participants p
                             WHERE p.together.id = f.together.id
                               AND p.member.id = :currentMemberId
                               AND p.participantsStatus = com.back.domain.together.entity.ParticipantsStatus.PARTICIPATING
                         ))))
          )
        ORDER BY f.bookmarkCount DESC, f.createdAt DESC
        LIMIT :limit
        """)
    List<Feed> findTopByBookmarkCountWithVisibility(
            @Param("currentMemberId") Long currentMemberId,
            @Param("limit") int limit
    );
    
    // ========== 공지 피드 조회 ==========
    
    /**
     * 특정 Together의 공지 피드 목록 조회 (상단 고정된 것 우선, 최신순)
     * 
     * @param togetherId Together ID
     * @return 공지 피드 목록
     */
    List<Feed> findByTogether_IdAndFeedTypeAndDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc(
            Long togetherId,
            com.back.domain.feed.entity.FeedType feedType
    );
    
    /**
     * 특정 Together의 상단 고정된 공지 피드만 조회
     * 
     * @param togetherId Together ID
     * @return 상단 고정된 공지 피드 목록
     */
    List<Feed> findByTogether_IdAndFeedTypeAndIsPinnedTrueAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long togetherId,
            com.back.domain.feed.entity.FeedType feedType
    );
    
    /**
     * 특정 Together의 최대 pinOrder 값 조회 (다음 순서 계산용)
     * 
     * @param togetherId Together ID
     * @return 최대 pinOrder 값 (없으면 0)
     */
    @Query("SELECT COALESCE(MAX(f.pinOrder), 0) FROM Feed f " +
           "WHERE f.together.id = :togetherId AND f.isPinned = true AND f.deletedAt IS NULL")
    Integer findMaxPinOrderByTogetherId(@Param("togetherId") Long togetherId);
    
    // ========== 통계 ==========
    
    /**
     * 전체 피드 개수 (삭제된 것 제외)
     */
    Long countByDeletedAtIsNull();
    
    /**
     * 특정 회원이 작성한 피드 개수
     * 
     * @param memberId 회원 ID
     * @return 작성한 피드 개수
     */
    Long countByMemberIdAndDeletedAtIsNull(Long memberId);
    
    // ========== 하루 1회 인증 체크 ==========
    
    /**
     * 특정 회원이 특정 함께하기에 오늘 인증한 피드 개수
     */
    @Query("SELECT COUNT(f) FROM Feed f " +
           "WHERE f.member.id = :memberId " +
           "AND f.together.id = :togetherId " +
           "AND f.feedType = 'TOGETHER_VERIFICATION' " +
           "AND f.createdAt >= :startOfDay " +
           "AND f.deletedAt IS NULL")
    Long countTodayVerificationByMemberAndTogether(
            @Param("memberId") Long memberId,
            @Param("togetherId") Long togetherId,
            @Param("startOfDay") java.time.LocalDateTime startOfDay
    );

    // ========== progress 계산 ==========

    /**
     * 특정 함께하기의 전체 멤버 인증 피드 총 개수 (progress 계산용)
     *
     * @param togetherId 함께하기 ID
     * @return 인증 피드 총 개수
     */
    @Query("SELECT COUNT(f) FROM Feed f " +
           "WHERE f.together.id = :togetherId " +
           "AND f.feedType = com.back.domain.feed.entity.FeedType.TOGETHER_VERIFICATION " +
           "AND f.deletedAt IS NULL")
    Long countVerificationByTogether(
            @Param("togetherId") Long togetherId
    );
}
