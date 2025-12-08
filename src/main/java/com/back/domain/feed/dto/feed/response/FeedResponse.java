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
    private List<String> tags;
    private FeedVisibility visibility;

    // 카운트
    private Integer reactionCount;
    private Integer commentCount;
    private Integer bookmarkCount;
    
    // 작성자 정보 (Member 엔티티 연결 후 추가 - 수정 가능성 많음.)
    // private MemberSummaryResponse author;
    
    // 함께하기 정보 (Together 엔티티 연결 후 추가)
    private Long togetherId;
    // private TogetherSummaryResponse together;
    
    // 현재 사용자의 리액션/북마크 여부
    private Boolean isReacted;        // 현재 사용자가 좋아요 눌렀는지
    private Boolean isBookmarked;     // 현재 사용자가 북마크했는지
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환 (정적 팩토리 메서드)
     */
    public static FeedResponse from(Feed feed) {
        return FeedResponse.builder()
                .id(feed.getId())
                .feedType(feed.getFeedType())
                .content(feed.getContent())
                .images(feed.getImages().stream()
                        .map(FeedImageResponse::from)
                        .collect(Collectors.toList()))
                .tags(feed.getTags())
                .visibility(feed.getVisibility())
                .reactionCount(feed.getReactionCount())
                .commentCount(feed.getCommentCount())
                .bookmarkCount(feed.getBookmarkCount())
                // .author(MemberSummaryResponse.from(feed.getMember()))
                .togetherId(null)  // feed.getTogether() != null ? feed.getTogether().getId() : null
                .isReacted(false)  // Service에서 설정 필요
                .isBookmarked(false)  // Service에서 설정 필요
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
                .tags(response.getTags())
                .visibility(response.getVisibility())
                .reactionCount(response.getReactionCount())
                .commentCount(response.getCommentCount())
                .bookmarkCount(response.getBookmarkCount())
                .togetherId(response.getTogetherId())
                .isReacted(isReacted)
                .isBookmarked(isBookmarked)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}
