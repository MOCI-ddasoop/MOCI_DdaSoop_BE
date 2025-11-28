package com.back.domain.together.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Together extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "mode")
    private String mode;

    @Column(name = "capacity")
    private int capacity;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "organizer_id")
//    private Member member;

    @Column(name = "status")
    private String status;
}
