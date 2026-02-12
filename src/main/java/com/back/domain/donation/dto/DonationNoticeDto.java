package com.back.domain.donation.dto;

import com.back.domain.donation.entity.DonationNotice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public class DonationNoticeDto {

    /* ===================Request====================== */

    // 후원하기 공지 게시
    public record CreateRequest(
            @Schema(description = "공지 제목", example = "공지 제목 예시")
            @NotBlank String title,

            @Schema(description = "공지 내용", example = "임시 공지 내용!")
            String description,

            @Schema(description = "진행 소식", example = "임시 진행 소식!")
            String progressNews,

            @Schema(description = "후기", example = "임시 후기!")
            String reviews,

            @Schema(description = "연결된 후원하기 ID", example = "1")
            @NotNull Long donationId
    ) {}

    /* ===================Response===================== */

    public record CreateResponse(
            Long id,
            String title,
            String description,
            String progressNews,
            String reviews,
            Long donationId
    ) {
        public static CreateResponse from(DonationNotice donationNotice) {
            return new CreateResponse(
                    donationNotice.getId(),
                    donationNotice.getTitle(),
                    donationNotice.getDescription(),
                    donationNotice.getProgressNews(),
                    donationNotice.getReviews(),
                    donationNotice.getDonations().getId()
            );
        }
    }
}
