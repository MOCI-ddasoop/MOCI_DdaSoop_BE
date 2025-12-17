package com.back.domain.feed.repository;

import com.back.domain.feed.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 - 단순 조회: Spring Data JPA 메서드
 - 복잡한 검색: QueryDSL (FeedRepositoryCustom)
 - Spring Data JPA: 단순 CRUD, 단건 조회, 단순 조건 조회
 */
public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {

    // ========== 기본 조회 (Spring Data JPA) ==========
    
    /**
     * ID로 피드 단건 조회 ( 삭제되지 않은 것만 )
     * 
     * @param id 피드 ID
     * @return Optional<Feed>
     * 
     * 사용 예:
     * - 피드 상세 페이지
     * - 피드 수정 전 조회
     * - 피드 삭제 전 조회
     */
    Optional<Feed> findByIdAndDeletedAtIsNull(Long id);
    
    /**
     * 무한 스크롤 (커서 기반 페이징)
     * 21개를 조회하여 hasNext 판단 (실제로는 20개만 반환)
     * 
     * @param lastFeedId 마지막으로 조회한 피드 ID
     * @return 최대 21개 피드 리스트
     * 
     * 사용 예:
     * - 모바일 앱 무한 스크롤
     * - 첫 조회: lastFeedId = Long.MAX_VALUE
     * - 두 번째: lastFeedId = 마지막 피드의 ID
     */
    List<Feed> findTop21ByIdLessThanAndDeletedAtIsNullOrderByIdDesc(Long lastFeedId);
    
    /**
     * 전체 피드 개수 (삭제된 것 제외)
     * 
     * @return 피드 개수
     * 
     * 사용 예:
     * - 통계 페이지: "전체 게시물 12,345개"
     * - 대시보드
     */
    Long countByDeletedAtIsNull();
}
