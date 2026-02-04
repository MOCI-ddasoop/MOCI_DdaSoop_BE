package com.back.domain.together.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Together extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private TogetherCategory category;

    @Column(name = "mode")
    @Enumerated(EnumType.STRING)
    private TogetherMode mode;

    @Column(name = "capacity")
    private Long capacity;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id") // 함께하기 만든 사람 && 운영자, 주최자, 소유자
    private Member member;

    @OneToMany(mappedBy = "together", fetch = FetchType.LAZY)// 함께하기 참가자들
    private List<Participants> participants = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TogetherStatus togetherStatus;
}
