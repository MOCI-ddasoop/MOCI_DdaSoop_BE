package com.back.domain.donation.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Donations extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "goal_amount")
    private Long goalAmount;

    @Column(name = "current_amount")
    private Long currentAmount = 0L;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "thumbnail_image_url")
    private String thumbnailImageUrl;

    @Builder.Default
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DonationStatus status = DonationStatus.RECRUITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private Member member;

    @OneToMany(mappedBy = "donations", fetch = FetchType.LAZY)
    private List<DonationParticipants> donationParticipants = new ArrayList<>();

    @Column(name = "donation_category")
    @Enumerated(EnumType.STRING)
    private DonationCategory donationCategory;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_notice_id")
    private DonationNotice donationNotice;

    public void increaseAmount(Long amount) {
        this.currentAmount += amount;
    }
}
