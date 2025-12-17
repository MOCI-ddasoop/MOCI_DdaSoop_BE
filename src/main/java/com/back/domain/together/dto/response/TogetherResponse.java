package com.back.domain.together.dto.response;

import com.back.domain.together.entity.Together;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TogetherResponse {
    private Long id;
    private String title;
    private String description;
    private TogetherCategory category;
    private TogetherMode mode;
    private Integer capacity;
    private TogetherStatus status;
    private Long organizerId;

    public static TogetherResponse from(Together together) {
        return TogetherResponse.builder()
                .id(together.getId())
                .title(together.getTitle())
                .description(together.getDescription())
                .category(together.getCategory())
                .mode(together.getMode())
                .capacity(together.getCapacity())
                .status(together.getStatus())
                .organizerId(together.getMember().getId())
                .build();
    }
}
