package com.back.domain.feed.dto.feed.request;

import com.back.domain.feed.entity.FeedVisibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 피드 수정 요청 DTO
 * 모든 필드가 선택사항 (수정하고 싶은 것만 보냄)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedUpdateRequest {

    @Size(max = 2000, message = "내용은 최대 2000자까지 입력 가능합니다.")
    private String content;

    @Size(max = 10, message = "이미지는 최대 10개까지 업로드 가능합니다.")
    @Builder.Default
    private List<FeedImageRequest> images = new ArrayList<>();

    @Size(max = 30, message = "태그는 최대 30개까지 입력 가능합니다.")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private FeedVisibility visibility;
}
