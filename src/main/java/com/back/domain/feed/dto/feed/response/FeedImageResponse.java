package com.back.domain.feed.dto.feed.response;

import com.back.domain.feed.entity.FeedImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드 이미지 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedImageResponse {

    private Long id;
    private String imageUrl;
    private Integer width;
    private Integer height;
    private Integer displayOrder;
    private Long fileSize;
    private String originalFileName;

    /**
     * Entity -> DTO 변환
     */
    public static FeedImageResponse from(FeedImage feedImage) {
        return FeedImageResponse.builder()
                .id(feedImage.getId())
                .imageUrl(feedImage.getImageUrl())
                .width(feedImage.getWidth())
                .height(feedImage.getHeight())
                .displayOrder(feedImage.getDisplayOrder())
                .fileSize(feedImage.getFileSize())
                .originalFileName(feedImage.getOriginalFileName())
                .build();
    }
}
