package com.back.domain.donation.dto;

import com.back.domain.donation.entity.DonationCategory;
import com.back.domain.donation.entity.DonationStatus;
import com.back.domain.donation.entity.Donations;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DonationDto {

    /* ===================Request====================== */

    // 후원하기 게시
    public record CreateRequest(
            @NotBlank String title,
            String description,
            @NotNull Long goalAmount,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            DonationStatus status,
            @NotNull DonationCategory category,
            Long memberId,
            @Size(max = 5, message = "최대 5개의 이미지 URL을 허용합니다.")
            List<String> imageUrls
    ) {}

    /* ===================Response===================== */

    public record ListResponse( // 전체 조회
            Long id,
            String title,
            Long goalAmount,
            Long currentAmount,
            LocalDate endDate,
            DonationStatus status,
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
                    donations.getImageUrls().getFirst(),
                    donations.getDonationCategory(),
                    dDay
            );
        }
        public static ListResponse fakeCard() {
            return new ListResponse(
                    -1L,"샘플 모금함",1000000L,500000L,LocalDate.now().plusDays(10),
                    null, null, DonationCategory.ANIMAL,10L
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
            DonationStatus status,
            List<String> imageUrls,
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
                    donations.getImageUrls(),
                    donations.getDonationCategory(),
                    dDay
            );
        }
    }

    // 마이페이지용 ID별 상세 조회
    public record MyDonationResponse(
            Long id,
            String title,
            Long amount,
            boolean isOwner
    ) {
        public static MyDonationResponse from(Donations donations, Long memberId){

            boolean isOwner = donations.getMember().getId().equals(memberId);

            return new MyDonationResponse(
                    donations.getId(),
                    donations.getTitle(),
                    donations.getCurrentAmount(),
                    isOwner
            );
        }
    }

    // 개설한 후원 리스트 조회
    public record MyDonationListResponse(
            Long id,
            String title,
            Long goalAmount,
            Long currentAmount,
            LocalDate endDate,
            String status,
            String thumbnailImage,
            String category,
            Long dDay
    ) {
        public static MyDonationListResponse from(Donations donations){

            LocalDate today = LocalDate.now();
            long dDay = ChronoUnit.DAYS.between(today, donations.getEndDate());

            return new MyDonationListResponse(
                    donations.getId(),
                    donations.getTitle(),
                    donations.getGoalAmount(),
                    donations.getCurrentAmount(),
                    donations.getEndDate(),
                    donations.getStatus().name(),
                    donations.getImageUrls().getFirst(),
                    donations.getDonationCategory().name(),
                    dDay
            );
        }
    }


    public record DescriptionResponse(String description) {}

    public record CreateResponse(
            Long id,
            String title,
            String description,
            Long goalAmount,
            LocalDate startDate,
            LocalDate endDate,
            String thumbnailImageUrl,
            DonationStatus status,
            DonationCategory category,
            Long memberId
    ) {
        public static CreateResponse from(Donations donations){
            return new CreateResponse(
                    donations.getId(),
                    donations.getTitle(),
                    donations.getDescription(),
                    donations.getGoalAmount(),
                    donations.getStartDate(),
                    donations.getEndDate(),
                    donations.getImageUrls().getFirst(),
                    donations.getStatus(),
                    donations.getDonationCategory(),
                    donations.getMember().getId()
            );
        }
    }
}
