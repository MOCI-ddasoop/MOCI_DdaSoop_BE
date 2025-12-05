package com.back.domain.feed.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 피드 리액션(좋아요) 엔티티
 * Member와 Feed의 다대다 관계를 중간 테이블로 표현
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "feed_reaction",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_feed_reaction_member_feed",
            columnNames = {"member_id", "feed_id"}
        )
    },
    indexes = {
        @Index(name = "idx_feed_reaction_feed_id", columnList = "feed_id"),
        @Index(name = "idx_feed_reaction_member_id", columnList = "member_id")
    }
)
public class FeedReaction extends BaseEntity {

    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                 // 리액션을 누른 사용자
    */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;                     // 리액션 대상 피드

    // ========== 비즈니스 로직 ==========

    // 리액션 생성 시 Feed의 카운트 증가
    @PrePersist
    public void prePersist() {
        if (this.feed != null) {
            this.feed.incrementReactionCount();
        }
    }

    // 리액션 삭제 시 Feed의 카운트 감소
    @PreRemove
    public void preRemove() {
        if (this.feed != null) {
            this.feed.decrementReactionCount();
        }
    }
}
