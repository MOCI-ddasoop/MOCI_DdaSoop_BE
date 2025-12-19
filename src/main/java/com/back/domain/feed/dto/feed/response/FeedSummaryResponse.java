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
    private Integer imageCount;          // 전체 이미지 개수
    private List<String> tags;           // 태그 목록
    
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
    
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static FeedSummaryResponse from(Feed feed) {
        return FeedSummaryResponse.builder()
                .id(feed.getId())
                .feedType(feed.getFeedType())
                .content(feed.getContent())
                .thumbnailUrl(feed.getFirstImageUrl())
                .imageCount(feed.getImageCount())
                .tags(feed.getTags())
                .reactionCount(feed.getReactionCount())
                .commentCount(feed.getCommentCount())
                .bookmarkCount(feed.getBookmarkCount())
                .authorId(feed.getMember().getId())
                .authorName(feed.getMember().getName())
                .authorNickname(feed.getMember().getNickname())
                .authorProfileImage(feed.getMember().getProfileImageUrl())
                .togetherId(feed.getTogether() != null ? feed.getTogether().getId() : null)
                .togetherTitle(feed.getTogether() != null ? feed.getTogether().getTitle() : null)
                .createdAt(feed.getCreatedAt())
                .build();
    }
}
