package com.back.domain.together.dto.request;

import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherStatus;
import com.back.domain.member.entity.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TogetherRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private TogetherCategory category;

    @NotNull
    private TogetherMode mode;

    @NotNull
    private Integer capacity;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private Member member;
}
