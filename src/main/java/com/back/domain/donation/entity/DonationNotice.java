package com.back.domain.donation.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "donation_notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DonationNotice extends BaseEntity {

    private String title;
    private String description;
    private String progressNews;
    private String reviews;

    @OneToOne(fetch = FetchType.LAZY)
    private Donations donations;
}
