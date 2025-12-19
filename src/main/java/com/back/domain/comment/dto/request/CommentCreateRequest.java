package com.back.domain.comment.dto.request;

import com.back.domain.comment.entity.CommentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {

    @NotNull(message = "댓글 타입은 필수입니다.")
    private CommentType commentType;  // FEED, TOGETHER, DONATION

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 최대 1000자까지 입력 가능합니다.")
    private String content;

    /**
     * 대상 엔티티 ID
     * - commentType이 FEED이면 feedId
     * - commentType이 TOGETHER이면 togetherId
     * - commentType이 DONATION이면 donationId
     */
    @NotNull(message = "대상 ID는 필수입니다.")
    private Long targetId;

    /**
     * 부모 댓글 ID (대댓글인 경우)
     * - null이면 최상위 댓글
     * - not null이면 대댓글
     */
    private Long parentId;
}
