package com.back.domain.feed.dto.feed.response;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedType;
import com.back.domain.feed.entity.FeedVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 피드 상세 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponse {

    private Long id;
    private FeedType feedType;
    private String content;
    private List<FeedImageResponse> images;
    private String thumbnailUrl;         // 첫 번째 이미지 (썸네일)
    private Integer thumbnailWidth;      // 썸네일 가로 크기 (px)
    private Integer thumbnailHeight;     // 썸네일 세로 크기 (px)
    private Integer imageCount;          // 전체 이미지 개수
    private List<String> tags;
    private FeedVisibility visibility;

    // 카운트
    private Integer reactionCount;
    private Integer commentCount;
    private Integer bookmarkCount;
    
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
    
    // 현재 사용자의 리액션/북마크 여부
    private Boolean isReacted;        // 현재 사용자가 좋아요 눌렀는지
    private Boolean isBookmarked;     // 현재 사용자가 북마크했는지
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환 (정적 팩토리 메서드)
     */
    public static FeedResponse from(Feed feed) {
        // 첫 번째 이미지 정보 추출 (썸네일)
        String thumbnailUrl = null;
        Integer thumbnailWidth = null;
        Integer thumbnailHeight = null;
        
        if (feed.getFirstImage() != null) {
            thumbnailUrl = feed.getFirstImage().getImageUrl();
            thumbnailWidth = feed.getFirstImage().getWidth();
            thumbnailHeight = feed.getFirstImage().getHeight();
        }
        
        return FeedResponse.builder()
                .id(feed.getId())
                .feedType(feed.getFeedType())
                .content(feed.getContent())
                .images(feed.getImages().stream()
                        .map(FeedImageResponse::from)
                        .collect(Collectors.toList()))
                .thumbnailUrl(thumbnailUrl)
                .thumbnailWidth(thumbnailWidth)
                .thumbnailHeight(thumbnailHeight)
                .imageCount(feed.getImageCount())
                .tags(feed.getTags())
                .visibility(feed.getVisibility())
                .reactionCount(feed.getReactionCount())
                .commentCount(feed.getCommentCount())
                .bookmarkCount(feed.getBookmarkCount())
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
                .isReacted(false)
                .isBookmarked(false)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (현재 사용자의 리액션/북마크 정보 포함)
     */
    public static FeedResponse from(Feed feed, boolean isReacted, boolean isBookmarked) {
        FeedResponse response = from(feed);
        return FeedResponse.builder()
                .id(response.getId())
                .feedType(response.getFeedType())
                .content(response.getContent())
                .images(response.getImages())
                .thumbnailUrl(response.getThumbnailUrl())
                .thumbnailWidth(response.getThumbnailWidth())
                .thumbnailHeight(response.getThumbnailHeight())
                .imageCount(response.getImageCount())
                .tags(response.getTags())
                .visibility(response.getVisibility())
                .reactionCount(response.getReactionCount())
                .commentCount(response.getCommentCount())
                .bookmarkCount(response.getBookmarkCount())
                .authorId(response.getAuthorId())
                .authorName(response.getAuthorName())
                .authorNickname(response.getAuthorNickname())
                .authorProfileImage(response.getAuthorProfileImage())
                .togetherId(response.getTogetherId())
                .togetherTitle(response.getTogetherTitle())
                .togetherCategory(response.getTogetherCategory())
                .togetherMode(response.getTogetherMode())
                .isPinned(response.getIsPinned())
                .isReacted(isReacted)
                .isBookmarked(isBookmarked)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}
