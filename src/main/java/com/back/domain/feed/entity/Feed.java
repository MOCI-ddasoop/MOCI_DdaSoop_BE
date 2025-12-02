package com.back.domain.feed.entity;

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
public class Feed extends BaseEntity {

    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;                 // 작성자
    */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeedType feedType;             // 피드 타입 (일반 피드 / 함께하기 인증 피드)

    @Column(length = 2000)
    private String content;                // 피드 내용 (최대 2000자)

    // 이미지 목록 (최대 10개 제한 권장)
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<FeedImage> images = new ArrayList<>();

    // 태그 목록 (태그 이름 최대 50자)
    @ElementCollection
    @CollectionTable(name = "feed_tags", joinColumns = @JoinColumn(name = "feed_id"))
    @Column(name = "tag_name", length = 50)
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedVisibility visibility;     // 피드 공개 범위

    /*
    // 함께하기 인증 피드인 경우, 연결된 함께하기 모임 (null 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "together_id")
    private Together together;

    // 함께하기 카테고리 (null 가능)
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TogetherCategory category;
    */

    // ========== 카운트 필드 ==========
    @Column(nullable = false)
    private Integer reactionCount = 0;     // 리액션(좋아요) 수

    @Column(nullable = false)
    private Integer commentCount = 0;      // 댓글 수 (대댓글 포함)

    @Column(nullable = false)
    private Integer bookmarkCount = 0;     // 북마크(스크랩) 수

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

    // 댓글 카운트 증감
    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    // 북마크 카운트 증감
    public void incrementBookmarkCount() {
        this.bookmarkCount++;
    }

    public void decrementBookmarkCount() {
        if (this.bookmarkCount > 0) {
            this.bookmarkCount--;
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

    // 함께하기 인증 피드 여부 확인
    public boolean isTogetherVerificationFeed() {
        return this.feedType == FeedType.TOGETHER_VERIFICATION;
    }

    // 이미지 추가/삭제
    public void addImage(FeedImage image) {
        if (this.images.size() >= 10) {
            throw new IllegalStateException("이미지는 최대 10개까지 업로드 가능합니다.");
        }
        this.images.add(image);
    }

    public void removeImage(FeedImage image) {
        this.images.remove(image);
    }

    public void clearImages() {
        this.images.clear();
    }

    /**
     * 이미지 개수 반환
     */
    public int getImageCount() {
        return this.images.size();
    }

    /**
     * 첫 번째 이미지 URL 반환 (썸네일용)
     */
    public String getFirstImageUrl() {
        if (images.isEmpty()) {
            return null;
        }
        return images.get(0).getImageUrl();
    }

    // 태그 추가/삭제
    public void addTag(String tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    public void clearTags() {
        this.tags.clear();
    }

    // 피드 수정
    public void updateContent(String content) {
        this.content = content;
    }

    public void updateVisibility(FeedVisibility visibility) {
        this.visibility = visibility;
    }
}
