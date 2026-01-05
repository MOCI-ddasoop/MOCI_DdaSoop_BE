package com.back.domain.donation.dto.request;

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
public class DonationRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private Long goalAmount;

    @NotBlank
    private Long currentAmount;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private String status;
}
