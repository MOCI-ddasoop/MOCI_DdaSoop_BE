package com.back.domain.admin.dto.response;

import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.entity.CommentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommentSummaryResponse {

    private Long id;
    private CommentType commentType;
    private Long targetId;
    private Long authorId;
    private String authorNickname;
    private String contentPreview;
    private Integer reactionCount;
    private Long reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public static AdminCommentSummaryResponse from(Comment comment, Long reportCount) {
        String content = comment.getContent();
        String preview = content != null && content.length() > 50
                ? content.substring(0, 50)
                : content;

        return AdminCommentSummaryResponse.builder()
                .id(comment.getId())
                .commentType(comment.getCommentType())
                .targetId(comment.getTargetEntityId())
                .authorId(comment.getMember().getId())
                .authorNickname(comment.getMember().getNickname())
                .contentPreview(preview)
                .reactionCount(comment.getReactionCount())
                .reportCount(reportCount)
                .createdAt(comment.getCreatedAt())
                .deletedAt(comment.getDeletedAt())
                .build();
    }

    public boolean getIsDeleted() {
        return deletedAt != null;
    }
}

