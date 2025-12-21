package com.back.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 최대 1000자까지 입력 가능합니다.")
    private String content;
}
