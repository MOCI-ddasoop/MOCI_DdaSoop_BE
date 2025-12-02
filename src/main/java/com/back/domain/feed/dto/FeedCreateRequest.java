package com.back.domain.feed.dto;

import com.back.domain.feed.entity.FeedType;
import com.back.domain.feed.entity.FeedVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedCreateRequest {

    @NotNull(message = "피드 타입은 필수입니다.")
    private FeedType feedType;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 2000, message = "내용은 최대 2000자까지 입력 가능합니다.")
    private String content;

    @Size(max = 10, message = "이미지는 최대 10개까지 업로드 가능합니다.")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    // 프론트엔드에서 파싱한 태그 목록 (# 제외)
    @Size(max = 30, message = "태그는 최대 30개까지 입력 가능합니다.")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @NotNull(message = "공개 범위는 필수입니다.")
    private FeedVisibility visibility;

    // 함께하기 인증 피드인 경우에만
    private Long togetherId;
}
