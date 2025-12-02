package com.back.domain.feed.repository;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // ========== 기본 조회 ==========

    /**
     * 삭제되지 않은 피드 전체 조회 (페이징)
     */
    Page<Feed> findByDeletedAtIsNull(Pageable pageable);

    /**
     * ID로 삭제되지 않은 피드 조회
     */
    Optional<Feed> findByIdAndDeletedAtIsNull(Long id);


    // ========== 무한 스크롤 (커서 기반 페이징, 수정 가능성 다분하기 때문에 절취선) ==========

    /**
     * 무한 스크롤: ID 기준 (최신순)
     * @param lastFeedId 마지막으로 조회한 피드 ID (처음이면 null 또는 Long.MAX_VALUE)
     */
    List<Feed> findTop20ByIdLessThanAndDeletedAtIsNullOrderByIdDesc(Long lastFeedId);

    /**
     * 무한 스크롤: 생성일 기준 (최신순)
     * @param lastCreatedAt 마지막으로 조회한 피드의 생성일
     */
    List<Feed> findTop20ByCreatedAtLessThanAndDeletedAtIsNullOrderByCreatedAtDesc(
            LocalDateTime lastCreatedAt
    );

    // ========== 특정 회원의 피드 ==========

    /**
     * 특정 회원이 작성한 피드 조회 (마이페이지용)
     */
    Page<Feed> findByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);

    /**
     * 특정 회원의 피드 개수
     */
    Long countByMemberIdAndDeletedAtIsNull(Long memberId);

    /**
     * 특정 회원의 피드 무한 스크롤
     */
    List<Feed> findTop20ByMemberIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            Long memberId,
            Long lastFeedId
    );

    // ========== 피드 타입별 조회 ==========

    /**
     * 피드 타입별 조회 (일반 피드 / 인증 피드)
     */
    Page<Feed> findByFeedTypeAndDeletedAtIsNull(FeedType feedType, Pageable pageable);

    /**
     * 피드 타입별 무한 스크롤
     */
    List<Feed> findTop20ByFeedTypeAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            FeedType feedType,
            Long lastFeedId
    );

    // ========== 태그 검색 ==========

    /**
     * 특정 태그가 포함된 피드 검색
     * @param tag 검색할 태그
     */
    @Query("SELECT DISTINCT f FROM Feed f JOIN f.tags t WHERE t = :tag AND f.deletedAt IS NULL")
    List<Feed> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 여러 태그 중 하나라도 포함된 피드 검색 (OR 조건)
     * @param tags 검색할 태그 목록
     */
    @Query("SELECT DISTINCT f FROM Feed f JOIN f.tags t WHERE t IN :tags AND f.deletedAt IS NULL")
    List<Feed> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    /**
     * 모든 태그를 포함한 피드 검색 (AND 조건)
     */
    @Query("SELECT f FROM Feed f WHERE " +
            "f.deletedAt IS NULL AND " +
            "SIZE(f.tags) >= :tagCount AND " +
            "(:tag1 MEMBER OF f.tags) AND " +
            "(:tag2 MEMBER OF f.tags)")
    List<Feed> findByAllTags(
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tagCount") int tagCount,
            Pageable pageable
    );

    // ========== 함께하기 관련 ==========

    /**
     * 특정 Together의 인증 피드 조회
     */
    // List<Feed> findByTogetherIdAndFeedTypeAndDeletedAtIsNull(
    //         Long togetherId,
    //         FeedType feedType,
    //         Pageable pageable
    // );

    /**
     * 특정 Together의 인증 피드 개수
     */
    // Long countByTogetherIdAndFeedTypeAndDeletedAtIsNull(Long togetherId, FeedType feedType);

    // ========== 인기 피드 ==========

    /**
     * 리액션이 많은 인기 피드 조회
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByReactionCountDescCreatedAtDesc();

    /**
     * 댓글이 많은 피드 조회
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();

    /**
     * 북마크가 많은 피드 조회
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();

    /**
     * 특정 기간 내 인기 피드 (리액션 기준)
     */
    @Query("SELECT f FROM Feed f WHERE f.deletedAt IS NULL AND f.createdAt >= :startDate " +
            "ORDER BY f.reactionCount DESC, f.createdAt DESC")
    List<Feed> findPopularFeedsSince(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    // ========== 검색 ==========

    /**
     * 내용으로 피드 검색 (LIKE 검색)
     */
    @Query("SELECT f FROM Feed f WHERE f.content LIKE %:keyword% AND f.deletedAt IS NULL")
    List<Feed> searchByContent(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 내용 또는 태그로 피드 검색
     */
    @Query("SELECT DISTINCT f FROM Feed f LEFT JOIN f.tags t WHERE " +
            "(f.content LIKE %:keyword% OR t LIKE %:keyword%) AND f.deletedAt IS NULL")
    List<Feed> searchByContentOrTag(@Param("keyword") String keyword, Pageable pageable);

    // ========== 통계 ==========

    /**
     * 전체 피드 개수 (삭제된 것 제외)
     */
    Long countByDeletedAtIsNull();

    /**
     * 특정 날짜 이후 생성된 피드 개수
     */
    Long countByCreatedAtAfterAndDeletedAtIsNull(LocalDateTime createdAt);
}
