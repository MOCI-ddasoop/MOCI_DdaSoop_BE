package com.back.domain.feed.repository;

import com.back.domain.feed.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

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
    
    // ========== Top N 조회 (인기 피드) ==========
    
    /**
     * 댓글 많은 피드 Top 20
     * 
     * 사용 예:
     * - 홈 화면: "토론 많은 게시물"
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();
    
    /**
     * 북마크 많은 피드 Top 20
     * 
     * 사용 예:
     * - 홈 화면: "가장 많이 저장된 게시물"
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();
    
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
}
