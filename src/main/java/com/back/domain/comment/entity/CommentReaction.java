package com.back.domain.comment.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 댓글 리액션(좋아요) 엔티티
 * Member와 Comment의 다대다 관계를 중간 테이블로 표현
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "comment_reaction",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_comment_reaction_member_comment",
            columnNames = {"member_id", "comment_id"}
        )
    },
    indexes = {
        @Index(name = "idx_comment_reaction_comment_id", columnList = "comment_id"),
        @Index(name = "idx_comment_reaction_member_id", columnList = "member_id")
    }
)
public class CommentReaction extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                 // 리액션을 누른 사용자


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;               // 리액션 대상 댓글

    // ========== 비즈니스 로직 ==========

    // 리액션 생성 시 Comment의 카운트 증가
    @PrePersist
    public void prePersist() {
        if (this.comment != null) {
            this.comment.incrementReactionCount();
        }
    }

    // 리액션 삭제 시 Comment의 카운트 감소
    @PreRemove
    public void preRemove() {
        if (this.comment != null) {
            this.comment.decrementReactionCount();
        }
    }
}
