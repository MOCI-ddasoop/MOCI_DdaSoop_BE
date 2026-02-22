package com.back.domain.comment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 리액션 토글 응답 DTO
 * 리액션 추가/취소 시 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionResponse {

    /**
     * 현재 리액션 상태
     * true: 리액션 추가됨
     * false: 리액션 취소됨
     */
    private Boolean isReacted;

    /**
     * 현재 리액션 개수
     * 토글 후의 최신 개수
     */
    private Integer reactionCount;

    /**
     * 정적 팩토리 메서드
     */
    public static CommentReactionResponse of(boolean isReacted, int reactionCount) {
        return CommentReactionResponse.builder()
                .isReacted(isReacted)
                .reactionCount(reactionCount)
                .build();
    }
}
