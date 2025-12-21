package com.back.domain.donation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private int goalAmount;

    @NotBlank
    private int currentAmount;

    @NotBlank
    private LocalDate startDate;

    @NotBlank
    private LocalDate endDate;

    @NotBlank
    private String status;
}
