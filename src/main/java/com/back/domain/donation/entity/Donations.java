package com.back.domain.donation.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
    private int goalAmount;

    @Column(name = "current_amount")
    private int currentAmount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status")
    private String status;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "organizer_id")
//    private Member member;
}
