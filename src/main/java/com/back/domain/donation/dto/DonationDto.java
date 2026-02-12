package com.back.domain.donation.dto;

import com.back.domain.donation.entity.DonationCategory;
import com.back.domain.donation.entity.Donations;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DonationDto {

    /* ===================Request====================== */
    /* ===================Response===================== */

    public record ListResponse( // 전체 조회
            Long id,
            String title,
            Long goalAmount,
            Long currentAmount,
            LocalDate endDate,
            String status,
            String thumbnailImage,
            DonationCategory category,
            Long dDay
    ) {
        public static ListResponse from(Donations donations){

            LocalDate today = LocalDate.now();
            long dDay = ChronoUnit.DAYS.between(today, donations.getEndDate());

            return new ListResponse(
                    donations.getId(),
                    donations.getTitle(),
                    donations.getGoalAmount(),
                    donations.getCurrentAmount(),
                    donations.getEndDate(),
                    donations.getStatus(),
                    null, // TODO: 추후 thumbnailImage 매핑
                    donations.getDonationCategory(),
                    dDay
            );
        }
        public static ListResponse fakeCard() {
            return new ListResponse(
                    -1L,"샘플 모금함",1000000L,500000L,LocalDate.now().plusDays(10),
                    "ONGOING", null, DonationCategory.ANIMAL,10L
            );
        }
    }

    public record PageResponse<T>( // 페이징 조회
            List<T> content,
            int page,
            int size,
            Long totalElements,
            int totalPages
    ) {}

    public record DetailResponse( // 상세 조회
            Long id,
            String title,
            String description,
            Long goalAmount,
            Long currentAmount,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String thumbnailImage,
            DonationCategory category,
            Long dDay
    ) {
        public static DetailResponse from(Donations donations){
            LocalDate today = LocalDate.now();
            long dDay = ChronoUnit.DAYS.between(today, donations.getEndDate());

            return new DetailResponse(
                    donations.getId(),
                    donations.getTitle(),
                    donations.getDescription(),
                    donations.getGoalAmount(),
                    donations.getCurrentAmount(),
                    donations.getStartDate(),
                    donations.getEndDate(),
                    donations.getStatus(),
                    null, // TODO: 추후 thumbnailImage 매핑
                    donations.getDonationCategory(),
                    dDay
            );
        }
    }

    public record DescriptionResponse(String description) {}
}
