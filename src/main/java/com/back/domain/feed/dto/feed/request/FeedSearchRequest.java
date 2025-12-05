package com.back.domain.feed.dto.feed.request;

import com.back.domain.feed.entity.FeedType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 피드 검색/필터링 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedSearchRequest {

    private String keyword;          // 내용 검색 키워드

    private List<String> tags;       // 태그 검색 (OR 조건)

    private FeedType feedType;       // 피드 타입 필터

    private Long memberId;           // 특정 회원의 피드만

    private Long togetherId;         // 특정 함께하기의 인증 피드만

    // 무한 스크롤용
    private Long lastFeedId;         // 마지막으로 조회한 피드 ID

    // 페이징용
    private Integer page;            // 페이지 번호 (0부터 시작)
    private Integer size;            // 페이지 크기 (기본 20)

    // 정렬 옵션
    private String sortBy;           // "latest"(최신순), "popular"(인기순), "comments"(댓글순)

    // 디폴트 값 처리 메서드들
    public int getPageOrDefault() {
        return page != null ? page : 0;
    }
    // size는 최대 100까지 허용, 기본 20 설정
    public int getSizeOrDefault() {
        return size != null && size > 0 && size <= 100 ? size : 20;
    }
    // 기본 정렬은 최신순
    public String getSortByOrDefault() {
        return sortBy != null ? sortBy : "latest";
    }
}
