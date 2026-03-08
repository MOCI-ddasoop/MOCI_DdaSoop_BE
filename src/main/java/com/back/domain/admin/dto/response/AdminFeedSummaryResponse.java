package com.back.domain.admin.dto.response;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedType;
import com.back.domain.feed.entity.FeedVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFeedSummaryResponse {

    private Long id;
    private FeedType feedType;
    private FeedVisibility visibility;
    private Long authorId;
    private String authorNickname;
    private String contentPreview;
    private Integer reactionCount;
    private Integer commentCount;
    private Integer bookmarkCount;
    private Long reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public static AdminFeedSummaryResponse from(Feed feed, Long reportCount) {
        String content = feed.getContent();
        String preview = content != null && content.length() > 50
                ? content.substring(0, 50)
                : content;

        return AdminFeedSummaryResponse.builder()
                .id(feed.getId())
                .feedType(feed.getFeedType())
                .visibility(feed.getVisibility())
                .authorId(feed.getMember().getId())
                .authorNickname(feed.getMember().getNickname())
                .contentPreview(preview)
                .reactionCount(feed.getReactionCount())
                .commentCount(feed.getCommentCount())
                .bookmarkCount(feed.getBookmarkCount())
                .reportCount(reportCount)
                .createdAt(feed.getCreatedAt())
                .deletedAt(feed.getDeletedAt())
                .build();
    }

    public boolean getIsDeleted() {
        return deletedAt != null;
    }
}

