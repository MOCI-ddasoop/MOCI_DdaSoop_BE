package com.back.domain.feed.dto.feed.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 업로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private String imageUrl;           // 업로드된 이미지 URL (Python 서버에서 제공)
    private Integer width;             // 이미지 가로 크기 (px)
    private Integer height;            // 이미지 세로 크기 (px)
    private Long fileSize;             // 파일 크기 (bytes)
    private String originalFileName;   // 원본 파일명
    private String savedFileName;      // 저장된 파일명 (UUID)
}
