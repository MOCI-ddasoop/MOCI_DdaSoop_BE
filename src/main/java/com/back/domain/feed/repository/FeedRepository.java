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
    
    // ========== 통계 ==========
    
    /**
     * 전체 피드 개수 (삭제된 것 제외)
     */
    Long countByDeletedAtIsNull();
}
