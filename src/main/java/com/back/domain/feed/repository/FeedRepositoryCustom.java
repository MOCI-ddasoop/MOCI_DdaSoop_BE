package com.back.domain.feed.repository;

import com.back.domain.feed.dto.feed.request.FeedSearchCondition;
import com.back.domain.feed.entity.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Feed QueryDSL Custom Repository 인터페이스
 * 복잡한 동적 쿼리를 위한 메서드 정의
 */
public interface FeedRepositoryCustom {
    
    /**
     * 동적 조건으로 피드 검색 (페이징)
     * 
     * @param condition 검색 조건 (feedType, memberId, tags, keyword, sortBy 등)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 피드 페이지
     * 
     * 사용 예:
     * - feedType만 → feedType 조건만 적용
     * - feedType + tags → 두 조건 모두 적용 (AND)
     * - tags가 여러개 → OR 조건으로 검색
     * - 조건 없음 → 전체 피드 조회
     */
    Page<Feed> searchFeeds(FeedSearchCondition condition, Pageable pageable);
    
    /**
     * 태그로 피드 검색 (동적 쿼리)
     * 
     * @param tags 검색할 태그 목록 (OR 조건)
     * @param pageable 페이징 정보
     * @return 피드 리스트
     * 
     * 사용 예:
     * - tags: ["여행", "맛집"] → 여행 OR 맛집 태그가 있는 피드
     */
    List<Feed> findByTagsWithDynamicQuery(List<String> tags, Pageable pageable);
    
    /**
     * 인기 피드 조회 (조건부)
     * 
     * @param condition 검색 조건 (기간, 타입 등)
     * @param limit 최대 개수
     * @return 인기 피드 리스트 (리액션 많은 순)
     * 
     * 사용 예:
     * - condition.startDate: 7일 전 → 최근 7일 인기 피드
     * - condition.feedType: GENERAL → 일반 피드 중 인기 피드
     */
    List<Feed> findPopularFeedsWithCondition(FeedSearchCondition condition, int limit);
    
    /**
     * 조건별 피드 개수
     * 
     * @param condition 검색 조건
     * @return 피드 개수
     * 
     * 사용 예:
     * - "총 검색 결과: 123개" 표시용
     */
    Long countByCondition(FeedSearchCondition condition);
}
