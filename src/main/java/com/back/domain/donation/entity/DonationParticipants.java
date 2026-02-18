package com.back.domain.donation.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "donation_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder

public class DonationParticipants extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id") // 참가자
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id") // 참가한 후원하기
    private Donations donations;

    @Column(name = "join_at")
    private LocalDateTime joinAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "participants_status")
    @Enumerated(EnumType.STRING)
    private DonationParticipantStatus participantsStatus;

    @Column(name = "participant_role")
    @Enumerated(EnumType.STRING)
    private DonationParticipantRole participantRole;

    public static DonationParticipants create(Donations donations) {
        Member member = donations.getMember();
        DonationParticipants dp = new DonationParticipants();
        dp.member = member;
        dp.donations = donations;
        dp.joinAt = LocalDateTime.now();
        dp.participantsStatus = DonationParticipantStatus.PARTICIPATING;
        dp.participantRole = DonationParticipantRole.LEADER;
        donations.getDonationParticipants().add(dp);
        return dp;
    }

    public void participate(){
        if (this.participantsStatus == DonationParticipantStatus.PARTICIPATING) {
            throw new IllegalStateException("이미 참여 중인 참가자입니다.");
        }
        if (this.participantsStatus == DonationParticipantStatus.DROPPED) {
            throw new IllegalStateException("강퇴된 참가자는 다시 참여할 수 없습니다.");
        }
        this.participantsStatus = DonationParticipantStatus.PARTICIPATING;
        this.joinAt = LocalDateTime.now();
    }

    public void leave(){
        if (this.participantsStatus != DonationParticipantStatus.PARTICIPATING) {
            throw new IllegalStateException("참여 중인 참가자만 탈퇴할 수 있습니다.");
        }
        this.participantsStatus = DonationParticipantStatus.LEAVED;
        this.leftAt = LocalDateTime.now();
    }

    public void drop(){
        switch (this.participantsStatus) {
            case PARTICIPATING-> {
                this.participantsStatus = DonationParticipantStatus.DROPPED;
                this.leftAt = LocalDateTime.now();
            }
            case LEAVED -> throw new IllegalStateException("탈퇴한 참가자는 강퇴할 수 없습니다.");
            case DROPPED -> throw new IllegalStateException("이미 강퇴된 참가자입니다.");
        }
    }
}
