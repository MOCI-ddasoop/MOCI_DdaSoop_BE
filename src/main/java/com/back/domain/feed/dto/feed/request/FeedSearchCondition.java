package com.back.domain.feed.dto.feed.request;

import com.back.domain.feed.entity.FeedType;
import com.back.domain.feed.entity.FeedVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QueryDSL 동적 쿼리를 위한 검색 조건 DTO
 * FeedSearchRequest를 받아서 이 DTO로 변환하여 Repository에 전달
 * 
 * Repository 계층에서 사용하는 내부 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedSearchCondition {
    
    /**
     * 피드 타입 필터
     * GENERAL(일반 피드) 또는 TOGETHER_VERIFICATION(함께하기 인증 피드)
     */
    private FeedType feedType;
    
    /**
     * 작성자 ID
     * 특정 회원이 작성한 피드만 조회할 때 사용
     */
    private Long memberId;
    
    /**
     * 태그 검색 (OR 조건)
     * 리스트에 포함된 태그 중 하나라도 있으면 검색됨
     */
    private List<String> tags;
    
    /**
     * 내용 검색 (LIKE 검색)
     * 피드 content에 키워드가 포함된 피드 검색
     */
    private String keyword;
    
    /**
     * 정렬 기준
     * "latest" (최신순, 기본값)
     * "popular" (인기순 - 리액션 많은 순)
     * "comments" (댓글순 - 댓글 많은 순)
     * "bookmarks" (북마크순 - 북마크 많은 순)
     */
    private String sortBy;
    
    /**
     * 기간 검색 - 시작 날짜
     * 이 날짜 이후에 생성된 피드만 조회
     */
    private LocalDateTime startDate;
    
    /**
     * 기간 검색 - 종료 날짜
     * 이 날짜 이전에 생성된 피드만 조회
     */
    private LocalDateTime endDate;
    
    /**
     * 공개 범위 필터
     * PUBLIC(전체 공개), FOLLOWERS(팔로워 공개), PRIVATE(비공개)
     */
    private FeedVisibility visibility;
    
    /**
     * 함께하기 ID
     * 특정 함께하기 모임의 인증 피드만 조회할 때 사용
     */
    private Long togetherId;
    
    /**
     * FeedSearchRequest에서 FeedSearchCondition으로 변환하는 정적 팩토리 메서드
     * Service 계층에서 사용
     */
    public static FeedSearchCondition from(FeedSearchRequest request) {
        return FeedSearchCondition.builder()
                .feedType(request.getFeedType())
                .memberId(request.getMemberId())
                .tags(request.getTags())
                .keyword(request.getKeyword())
                .sortBy(request.getSortByOrDefault())
                .visibility(null)  // 필요시 추가
                .togetherId(request.getTogetherId())
                .build();
    }
}
