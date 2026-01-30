package com.back.domain.comment.entity;

import lombok.Getter;

/**
 * 댓글 타입
 * 어느 도메인에 속한 댓글인지 구분
 */
@Getter
public enum CommentType {
    
    FEED("피드 댓글", "Feed 도메인의 댓글"),
    TOGETHER("함께하기 댓글", "Together 도메인의 댓글"),
    DONATION("기부하기 댓글", "Donation 도메인의 댓글");  // 미래 확장용
    
    private final String displayName;
    private final String description;
    
    CommentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
