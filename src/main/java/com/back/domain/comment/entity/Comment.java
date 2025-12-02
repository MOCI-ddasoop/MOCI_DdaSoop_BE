package com.back.domain.comment.entity;

import com.back.domain.feed.entity.Feed;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends BaseEntity {

    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                 // 댓글 작성자
    */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;                     // 댓글이 달린 피드

    @Column(length = 1000, nullable = false)
    private String content;                // 댓글 내용 (최대 1000자)

    // ========== 대댓글 구조 (자기 참조) ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;                // 부모 댓글 (null이면 최상위 댓글)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();  // 대댓글 목록

    // ========== 카운트 필드 ==========
    @Column(nullable = false)
    private Integer reactionCount = 0;     // 댓글 좋아요 수

    // ========== Soft Delete ==========
    @Column
    private LocalDateTime deletedAt;       // 삭제 시점 (null이면 삭제되지 않음)

    // ========== 비즈니스 로직 ==========

    // 리액션 카운트 증감
    public void incrementReactionCount() {
        this.reactionCount++;
    }

    public void decrementReactionCount() {
        if (this.reactionCount > 0) {
            this.reactionCount--;
        }
    }

    // Soft Delete
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // 최상위 댓글인지 확인
    public boolean isTopLevelComment() {
        return this.parent == null;
    }

    // 대댓글인지 확인
    public boolean isReply() {
        return this.parent != null;
    }

    // 댓글 수정
    public void updateContent(String content) {
        this.content = content;
    }

    // 대댓글 추가 (양방향 관계 편의 메서드)
    public void addReply(Comment reply) {
        this.replies.add(reply);
        // reply.parent = this; // 이 부분은 Builder나 생성 시점에서 설정되어야 함
    }

    // 피드의 댓글 카운트 업데이트 (댓글 생성 시)
    public void notifyFeedCommentCreated() {
        if (this.feed != null) {
            this.feed.incrementCommentCount();
        }
    }

    // 피드의 댓글 카운트 업데이트 (댓글 삭제 시)
    public void notifyFeedCommentDeleted() {
        if (this.feed != null) {
            this.feed.decrementCommentCount();
        }
    }
}
