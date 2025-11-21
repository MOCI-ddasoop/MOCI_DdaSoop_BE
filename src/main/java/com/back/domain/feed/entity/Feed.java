package com.back.domain.feed.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Feed extends BaseEntity {

    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                 // 작성자
    */

    @Column(length = 2000)
    private String content;                // 피드 내용 (일단 최대 2000자, 추후 프론트 결정 시, 수정 예정)

    // 이미지 목록
    @ElementCollection
    @CollectionTable(name = "feed_images", joinColumns = @JoinColumn(name = "feed_id"))
    @Column(name = "image_url")
    @OrderColumn(name = "display_order")
    private List<String> images = new ArrayList<>();

    // 태그 목록, 태그 이름은 최대 50자(임시), 추후 프론트와 협의 필요
    @ElementCollection
    @CollectionTable(name = "feed_tags", joinColumns = @JoinColumn(name = "feed_id"))
    @Column(name = "tag_name", length = 50)
    private List<String> tags = new ArrayList<>();

    /*
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedVisibility visibility;  // 피드 공개 범위

    // 함께하기 섹션에서 넘어오는 인증 피드 관련, (null 가능, null이면 일반 피드)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "together_id")
    private Together together;

    // 함께하기 섹션에서 넘어오는 카테고리, (null 가능, null이면 일반 피드)
    @Enumerated(EnumType.STRING)
    private TogetherCategory category;
    */

    // 카운트 필드
    @Column(nullable = false)
    private Integer reactionCount = 0;

    @Column(nullable = false)
    private Integer commentCount = 0;

    @Column(nullable = false)
    private Integer bookmarkCount = 0;

    // 카운트 필드에 대한 비즈니스 로직
    public void incrementReactionCount() {
        this.reactionCount++;
    }

    public void decrementReactionCount() {
        if (this.reactionCount > 0) {
            this.reactionCount--;
        }
    }

    public void incrementCommentCount() {
        this.reactionCount++;
    }

    public void decrementCommentCount() {
        if (this.reactionCount > 0) {
            this.reactionCount--;
        }
    }

    public void incrementBookmarkCount() {
        this.reactionCount++;
    }

    public void decrementBookmarkCount() {
        if (this.reactionCount > 0) {
            this.reactionCount--;
        }
    }

}
