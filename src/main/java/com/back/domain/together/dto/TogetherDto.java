package com.back.domain.together.dto;

import com.back.domain.member.entity.Member;
import com.back.domain.together.entity.Participants;
import com.back.domain.together.entity.Together;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class TogetherDto {

    /* ===================Request====================== */

    public record ListRequest(
            @NotBlank String title,
            @NotNull TogetherCategory category,
            @NotNull TogetherMode mode,
            @NotNull Long capacity,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            Member member
    ){}

    /* ===================Response======================= */

    public record ListResponse(
            Long id,
            String title,
            TogetherCategory category,
            TogetherMode mode,
            Long capacity,
            LocalDate startDate,
            LocalDate endDate,
            Long memberId,
            Participants participants,
            String thumbnailImage,
            Long progress
    ) {
        public static ListResponse from(Together together) {
            return new ListResponse(
                    together.getId(),
                    together.getTitle(),
                    together.getCategory(),
                    together.getMode(),
                    together.getCapacity(),
                    together.getStartDate(),
                    together.getEndDate(),
                    together.getMember().getId(),
                    null,
                    null,
                    null
            );
        }
    }

    public record DetailResponse(
            Long id,
            String title,
            TogetherCategory category,
            TogetherMode mode,
            Long capacity,
            LocalDate startDate,
            LocalDate endDate,
            Long memberId,
            Participants participants,
            String thumbnailImage,
            Long goal,
            Long progress
    ) {
        public static DetailResponse from(Together together) {
            return new DetailResponse(
                    together.getId(),
                    together.getTitle(),
                    together.getCategory(),
                    together.getMode(),
                    together.getCapacity(),
                    together.getStartDate(),
                    together.getEndDate(),
                    together.getMember().getId(),
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
