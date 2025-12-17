package com.back.domain.feed.dto.feed.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 무한 스크롤 응답 DTO
 * 커서 기반 페이징을 위한 응답 형식
 * 
 * @param <T> 실제 데이터 타입 (예: FeedSummaryResponse)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfiniteScrollResponse<T> {

    /**
     * 실제 데이터 목록 (최대 20개)
     */
    private List<T> content;

    /**
     * 다음 페이지 요청 시 사용할 커서 (마지막 항목의 ID)
     * null이면 마지막 페이지
     */
    private Long nextCursor;

    /**
     * 다음 페이지 존재 여부
     * true: 더 가져올 데이터가 있음
     * false: 마지막 페이지
     */
    private boolean hasNext;

    /**
     * 현재 페이지의 실제 데이터 개수
     */
    private int size;

    /**
     * 요청한 페이지 크기 (기본 20)
     */
    private int requestedSize;

    /**
     * 정적 팩토리 메서드 - 제네릭 타입 추론을 위해
     */
    public static <T> InfiniteScrollResponse<T> of(
            List<T> content, 
            Long nextCursor, 
            boolean hasNext
    ) {
        return InfiniteScrollResponse.<T>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(content.size())
                .requestedSize(20)  // 기본값
                .build();
    }
}
