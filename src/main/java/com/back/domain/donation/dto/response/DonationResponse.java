package com.back.domain.donation.dto.response;

import com.back.domain.donation.entity.Donations;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationResponse {

    private String title;
    private String description;
    private int goalAmount;
    private int currentAmount;
    private String startDate;
    private String endDate;
    private String status;

    public static DonationResponse from(Donations donations) {
        return DonationResponse.builder()
                .title(donations.getTitle())
                .description(donations.getDescription())
                .goalAmount(donations.getGoalAmount())
                .currentAmount(donations.getCurrentAmount())
                .startDate(donations.getStartDate().toString())
                .endDate(donations.getEndDate().toString())
                .status(donations.getStatus())
                .build();
    }
}
