package com.back.domain.feed.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 피드 북마크(스크랩) 엔티티
 * Member와 Feed의 다대다 관계를 중간 테이블로 표현
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "feed_bookmark",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_feed_bookmark_member_feed",
            columnNames = {"member_id", "feed_id"}
        )
    },
    indexes = {
        @Index(name = "idx_feed_bookmark_feed_id", columnList = "feed_id"),
        @Index(name = "idx_feed_bookmark_member_id", columnList = "member_id")
    }
)
public class FeedBookmark extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                 // 북마크한 사용자


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;                     // 북마크 대상 피드

    // ========== 비즈니스 로직 ==========

    // 북마크 생성 시 Feed의 카운트 증가
    @PrePersist
    public void prePersist() {
        if (this.feed != null) {
            this.feed.incrementBookmarkCount();
        }
    }

    // 북마크 삭제 시 Feed의 카운트 감소
    @PreRemove
    public void preRemove() {
        if (this.feed != null) {
            this.feed.decrementBookmarkCount();
        }
    }
}
