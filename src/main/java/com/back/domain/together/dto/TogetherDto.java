package com.back.domain.together.dto;

import com.back.domain.together.entity.Participants;
import com.back.domain.together.entity.Together;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TogetherDto {

    /* ===================Request====================== */

    public record CreateRequest(
            @NotBlank String title,
            String description,
            @NotNull TogetherCategory category,
            @NotNull TogetherMode mode,
            Long capacity,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            Long memberId
    ) {
    }

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
            Long progress,
            Long dDay
    ) {
        public static ListResponse from(Together together) {

            LocalDate today = LocalDate.now();
            long dDay = ChronoUnit.DAYS.between(today, together.getEndDate());

            return new ListResponse(
                    together.getId(),
                    together.getTitle(),
                    together.getCategory(),
                    together.getMode(),
                    together.getCapacity(),
                    together.getStartDate(),
                    together.getEndDate(),
                    together.getMember().getId(),
                    null, // TODO: participants 정보 매핑
                    null, // TODO: thumbnailImage 정보 매핑
                    null, // TODO: progress 정보 매핑
                    dDay
            );
        }
        public static ListResponse fakeCard() {
            return new ListResponse(
                    -1L,"새 함께하기 추가",null,null,0L,null,
                    null,null,null,null,null,null
            );
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            Long totalElements,
            int totalPages
    ) {}

    // 함께하기 1페이지의 1번에 들어갈 가짜 카드


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
                    null, //TODO: 추후에 participants 정보 매핑
                    null, // TODO: 추후에 thumbnailImage 정보 매핑
                    null, // TODO: 추후에 goal 정보 매핑
                    null // TODO: 추후에 progress 정보 매핑
            );
        }
    }

    public record DescriptionResponse(
            String description
    ) {
        public static DescriptionResponse from(Together together) {
            return new DescriptionResponse(
                    together.getDescription()
            );
        }
    }

    public record CreateResponse(
            Long id,
            String title,
            String description,
            TogetherCategory category,
            TogetherMode mode,
            Long capacity,
            LocalDate startDate,
            LocalDate endDate,
            Long memberId
    ) {
        public static CreateResponse from(Together together) {
            return new CreateResponse(
                    together.getId(),
                    together.getTitle(),
                    together.getDescription(),
                    together.getCategory(),
                    together.getMode(),
                    together.getCapacity(),
                    together.getStartDate(),
                    together.getEndDate(),
                    together.getMember().getId()
            );
        }
    }
}
