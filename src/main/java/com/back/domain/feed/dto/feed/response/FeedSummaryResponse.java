package com.back.domain.feed.dto.feed.response;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 피드 목록 조회용 간단한 응답 DTO
 * 목록에서는 모든 정보가 필요 없으므로 필수 정보만 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedSummaryResponse {

    private Long id;
    private FeedType feedType;
    private String content;              // 전체 내용
    private String thumbnailUrl;         // 첫 번째 이미지 (썸네일)
    private Integer thumbnailWidth;      // 썸네일 가로 크기 (px)
    private Integer thumbnailHeight;     // 썸네일 세로 크기 (px)
    private Integer imageCount;          // 전체 이미지 개수
    private List<String> tags;           // 태그 목록
    
    // 카운트
    private Integer reactionCount;
    private Integer commentCount;
    private Integer bookmarkCount;

    // 현재 사용자의 리액션/북마크 여부
    private Boolean isReacted;
    private Boolean isBookmarked;

    // 작성자 정보
    private Long authorId;
    private String authorName;
    private String authorNickname;
    private String authorProfileImage;
    
    // 함께하기 정보
    private Long togetherId;
    private String togetherTitle;
    private String togetherCategory;    // Together 카테고리 (PLOGGING, CLEANUP, RECYCLING)
    private String togetherMode;        // Together 모드 (ONLINE, OFFLINE)
    
    // 공지 피드 관련
    private Boolean isPinned;
    
    private LocalDateTime createdAt;
    private LocalDateTime contentUpdatedAt; // 내용/태그/이미지/공개범위 수정 시점 (최초엔 createdAt과 동일)

    /**
     * Entity -> DTO 변환 (비로그인 또는 isReacted/isBookmarked 불필요 시)
     */
    public static FeedSummaryResponse from(Feed feed) {
        return from(feed, java.util.Set.of(), java.util.Set.of());
    }

    /**
     * Entity -> DTO 변환 (현재 사용자의 리액션/북마크 정보 포함)
     * Service에서 배치 조회한 reactedFeedIds, bookmarkedFeedIds Set을 전달받아 사용
     */
    public static FeedSummaryResponse from(Feed feed, java.util.Set<Long> reactedFeedIds, java.util.Set<Long> bookmarkedFeedIds) {
        String thumbnailUrl = null;
        Integer thumbnailWidth = null;
        Integer thumbnailHeight = null;

        if (feed.getFirstImage() != null) {
            thumbnailUrl = feed.getFirstImage().getImageUrl();
            thumbnailWidth = feed.getFirstImage().getWidth();
            thumbnailHeight = feed.getFirstImage().getHeight();
        }

        return FeedSummaryResponse.builder()
                .id(feed.getId())
                .feedType(feed.getFeedType())
                .content(feed.getContent())
                .thumbnailUrl(thumbnailUrl)
                .thumbnailWidth(thumbnailWidth)
                .thumbnailHeight(thumbnailHeight)
                .imageCount(feed.getImageCount())
                .tags(feed.getTags())
                .reactionCount(feed.getReactionCount())
                .commentCount(feed.getCommentCount())
                .bookmarkCount(feed.getBookmarkCount())
                .isReacted(reactedFeedIds.contains(feed.getId()))
                .isBookmarked(bookmarkedFeedIds.contains(feed.getId()))
                .authorId(feed.getMember().getId())
                .authorName(feed.getMember().getName())
                .authorNickname(feed.getMember().getNickname())
                .authorProfileImage(feed.getMember().getProfileImageUrl())
                .togetherId(feed.getTogether() != null ? feed.getTogether().getId() : null)
                .togetherTitle(feed.getTogether() != null ? feed.getTogether().getTitle() : null)
                .togetherCategory(feed.getTogether() != null && feed.getTogether().getCategory() != null ?
                        feed.getTogether().getCategory().name() : null)
                .togetherMode(feed.getTogether() != null && feed.getTogether().getMode() != null ?
                        feed.getTogether().getMode().name() : null)
                .isPinned(feed.getIsPinned())
                .createdAt(feed.getCreatedAt())
                .contentUpdatedAt(feed.getContentUpdatedAt())
                .build();
    }
}
