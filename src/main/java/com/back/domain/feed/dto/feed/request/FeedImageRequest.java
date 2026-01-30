package com.back.domain.feed.dto.feed.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드 이미지 업로드 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedImageRequest {

    @NotBlank(message = "이미지 URL은 필수입니다.")
    private String imageUrl;

    @NotNull(message = "이미지 가로 크기는 필수입니다.")
    private Integer width;

    @NotNull(message = "이미지 세로 크기는 필수입니다.")
    private Integer height;

    private Integer displayOrder;  // null이면 자동으로 순서 지정

    private Long fileSize;  // 선택사항, 필요 시

    private String originalFileName;  // 선택사항, 필요 시
}
