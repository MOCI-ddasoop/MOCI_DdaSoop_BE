package com.back.domain.comment.entity;

import com.back.domain.donation.entity.Donations;
import com.back.domain.feed.entity.Feed;
import com.back.domain.member.entity.Member;
import com.back.domain.together.entity.Together;
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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                 // 댓글 작성자

    // ========== 댓글 타입 (어느 도메인의 댓글인지) ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false, length = 20)
    private CommentType commentType;       // FEED, TOGETHER, DONATION

    // ========== 각 도메인별 참조 (nullable, 하나만 not null) ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id")
    private Feed feed;                     // Feed 댓글인 경우

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "together_id")
    private Together together;             // Together 댓글인 경우
    
    // Donations 등 다른 도메인 추가 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    private Donations donation;

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

    // 피드의 댓글 카운트 업데이트 (Feed 댓글 생성 시)
    public void notifyFeedCommentCreated() {
        if (this.commentType == CommentType.FEED && this.feed != null) {
            this.feed.incrementCommentCount();
        }
    }

    // 피드의 댓글 카운트 업데이트 (Feed 댓글 삭제 시)
    public void notifyFeedCommentDeleted() {
        if (this.commentType == CommentType.FEED && this.feed != null) {
            this.feed.decrementCommentCount();
        }
    }

    // ========== 엔티티 검증 ==========
    
    /**
     * Comment가 정확히 하나의 도메인에만 연결되어 있는지 검증
     * commentType과 실제 참조 엔티티가 일치하는지 검증
     */
    @PrePersist
    @PreUpdate
    private void validate() {
        // 1. 정확히 하나의 도메인 엔티티만 not null이어야 함
        int targetCount = 0;
        if (feed != null) targetCount++;
        if (together != null) targetCount++;
        if (donation != null) targetCount++;
        
        if (targetCount != 1) {
            throw new IllegalStateException(
                "Comment must belong to exactly one domain entity. " +
                "Current count: " + targetCount
            );
        }
        
        // 2. commentType과 실제 엔티티가 일치하는지 확인
        switch (commentType) {
            case FEED:
                if (feed == null) {
                    throw new IllegalStateException(
                        "CommentType is FEED but feed entity is null"
                    );
                }
                break;
            case TOGETHER:
                if (together == null) {
                    throw new IllegalStateException(
                        "CommentType is TOGETHER but together entity is null"
                    );
                }
                break;
            case DONATION:
                if (donation == null) {
                    throw new IllegalStateException(
                        "CommentType is DONATION but donation entity is null"
                    );
                }
                break;
            default:
                throw new IllegalStateException(
                    "Unknown comment type: " + commentType
                );
        }
    }

    // ========== 헬퍼 메서드 ==========
    
    /**
     * Feed 댓글인지 확인
     */
    public boolean isFeedComment() {
        return this.commentType == CommentType.FEED;
    }
    
    /**
     * Together 댓글인지 확인
     */
    public boolean isTogetherComment() {
        return this.commentType == CommentType.TOGETHER;
    }
    
    /**
     * 댓글이 속한 도메인의 ID 반환
     */
    public Long getTargetEntityId() {
        return switch (commentType) {
            case FEED -> feed != null ? feed.getId() : null;
            case TOGETHER -> together != null ? together.getId() : null;
            case DONATION -> null;  // 미래 확장
        };
    }
}
