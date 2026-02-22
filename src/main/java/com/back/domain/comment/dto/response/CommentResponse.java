package com.back.domain.comment.dto.response;

import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.entity.CommentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 댓글 응답 DTO
 * 최상위 댓글과 대댓글 모두 이 DTO 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private CommentType commentType;
    private String content;

    // 작성자 정보
    private Long authorId;
    private String authorName;
    private String authorNickname;
    private String authorProfileImage;

    // 대상 엔티티 정보
    private Long targetId;  // feedId, togetherId, donationId

    // 부모 댓글 정보 (대댓글인 경우)
    private Long parentId;
    private boolean isReply;  // 대댓글 여부

    // 대댓글 목록 (최상위 댓글인 경우)
    private List<CommentResponse> replies;
    private Integer replyCount;  // 대댓글 개수

    // 카운트
    private Integer reactionCount;

    // 현재 사용자의 리액션 여부
    private Boolean isReacted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환 (대댓글 포함)
     * 비로그인 혹은 isReacted 정보 없이 변환 시 사용 (대댓글 isReacted는 항상 false)
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .commentType(comment.getCommentType())
                .content(comment.getContent())
                // 작성자 정보
                .authorId(comment.getMember().getId())
                .authorName(comment.getMember().getName())
                .authorNickname(comment.getMember().getNickname())
                .authorProfileImage(comment.getMember().getProfileImageUrl())
                // 대상 엔티티
                .targetId(comment.getTargetEntityId())
                // 부모 댓글
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .isReply(comment.isReply())
                // 대댓글 (최상위 댓글인 경우만, 대댓글의 isReacted는 false로 고정)
                .replies(comment.isTopLevelComment()
                    ? comment.getReplies().stream()
                        .filter(reply -> !reply.isDeleted())
                        .map(reply -> CommentResponse.fromWithoutReplies(reply, false))
                        .collect(Collectors.toList())
                    : null)
                .replyCount(comment.isTopLevelComment()
                    ? (int) comment.getReplies().stream()
                        .filter(reply -> !reply.isDeleted())
                        .count()
                    : null)
                // 카운트
                .reactionCount(comment.getReactionCount())
                .isReacted(false)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (현재 사용자의 리액션 정보 포함)
     * Service에서 배치 조회한 reactedCommentIds Set을 전달받아 최상위 댓글 + 대댓글 모두 정확하게 처리
     */
    public static CommentResponse from(Comment comment, Set<Long> reactedCommentIds) {
        return CommentResponse.builder()
                .id(comment.getId())
                .commentType(comment.getCommentType())
                .content(comment.getContent())
                // 작성자 정보
                .authorId(comment.getMember().getId())
                .authorName(comment.getMember().getName())
                .authorNickname(comment.getMember().getNickname())
                .authorProfileImage(comment.getMember().getProfileImageUrl())
                // 대상 엔티티
                .targetId(comment.getTargetEntityId())
                // 부모 댓글
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .isReply(comment.isReply())
                // 대댓글 (최상위 댓글인 경우만, 동일한 Set으로 isReacted 정확하게 처리)
                .replies(comment.isTopLevelComment()
                    ? comment.getReplies().stream()
                        .filter(reply -> !reply.isDeleted())
                        .map(reply -> CommentResponse.fromWithoutReplies(reply, reactedCommentIds.contains(reply.getId())))
                        .collect(Collectors.toList())
                    : null)
                .replyCount(comment.isTopLevelComment()
                    ? (int) comment.getReplies().stream()
                        .filter(reply -> !reply.isDeleted())
                        .count()
                    : null)
                // 카운트
                .reactionCount(comment.getReactionCount())
                .isReacted(reactedCommentIds.contains(comment.getId()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (대댓글 제외 - 목록 조회용, isReacted 포함)
     * Service에서 배치 조회한 reactedCommentIds.contains(id) 결과를 전달받아 사용
     */
    public static CommentResponse fromWithoutReplies(Comment comment, boolean isReacted) {
        return CommentResponse.builder()
                .id(comment.getId())
                .commentType(comment.getCommentType())
                .content(comment.getContent())
                .authorId(comment.getMember().getId())
                .authorName(comment.getMember().getName())
                .authorNickname(comment.getMember().getNickname())
                .authorProfileImage(comment.getMember().getProfileImageUrl())
                .targetId(comment.getTargetEntityId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .isReply(comment.isReply())
                .replies(null)  // 대댓글 제외
                .replyCount(comment.isTopLevelComment()
                    ? (int) comment.getReplies().stream()
                        .filter(reply -> !reply.isDeleted())
                        .count()
                    : null)
                .reactionCount(comment.getReactionCount())
                .isReacted(isReacted)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
