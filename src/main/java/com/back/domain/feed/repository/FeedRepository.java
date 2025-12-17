package com.back.domain.feed.repository;

import com.back.domain.feed.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Feed Repository ( Spring Data JPA 기반)
 */
public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {

    // ========== 기본 조회 (Spring Data JPA) ==========
    
    /**
     * ID로 삭제되지 않은 피드 단건 조회
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
    
    // ========== 무한 스크롤 (커서 기반 페이징) ==========
    
    /**
     * 전체 피드 무한 스크롤
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
     * 특정 회원의 피드 무한 스크롤 (커서 기반)
     * 
     * @param memberId 회원 ID
     * @param lastFeedId 마지막으로 조회한 피드 ID
     * @return 최대 21개 피드 리스트
     * 
     * 사용 예:
     * - 프로필 페이지: "홍길동님의 피드" 무한 스크롤
     * - 마이페이지: "내가 작성한 피드" 무한 스크롤
     */
    List<Feed> findTop21ByMemberIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
        Long memberId, 
        Long lastFeedId
    );
    
    /**
     * 특정 Together의 인증 피드 무한 스크롤 (커서 기반)
     * 
     * @param togetherId 함께하기 ID
     * @param lastFeedId 마지막으로 조회한 피드 ID
     * @return 최대 21개 피드 리스트
     * 
     * 사용 예:
     * - Together 상세 페이지: "30일 운동 챌린지" 인증 피드 무한 스크롤
     */
    List<Feed> findTop21ByTogetherIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
        Long togetherId, 
        Long lastFeedId
    );
    
    // ========== Top N 조회 (인기 피드) ==========
    
    /**
     * 댓글 많은 피드 Top N
     * 
     * @return 최대 20개 피드 리스트
     * 
     * 사용 예:
     * - 홈 화면: "토론 많은 게시물"
     * - 트렌딩 페이지
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();
    
    /**
     * 북마크 많은 피드 Top N
     * 
     * @return 최대 20개 피드 리스트
     * 
     * 사용 예:
     * - 홈 화면: "가장 많이 저장된 게시물"
     * - 베스트 게시물 페이지
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();
    
    // ========== 통계 ==========
    
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
