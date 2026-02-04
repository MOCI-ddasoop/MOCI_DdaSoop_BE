package com.back.domain.together.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Participants extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id") // 참가자
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "together_id") // 참가한 함께하기
    private Together together;

    @Column(name = "join_at")
    private LocalDateTime joinAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "participants_status")
    @Enumerated(EnumType.STRING)
    private ParticipantsStatus participantsStatus;

    public static Participants create(Together together, Member member) {
        Participants p = new Participants();
        p.together = together;
        p.member = member;
        p.participantsStatus = ParticipantsStatus.PARTICIPATING;
        p.joinAt = LocalDateTime.now();
        together.getParticipants().add(p);
        return p;
    }

    public void participate(){
        if (this.participantsStatus == ParticipantsStatus.PARTICIPATING) {
            throw new IllegalStateException("이미 참여 중인 참가자입니다.");
        }
        if (this.participantsStatus == ParticipantsStatus.DROPPED) {
            throw new IllegalStateException("강퇴된 참가자는 다시 참여할 수 없습니다.");
        }
        this.participantsStatus = ParticipantsStatus.PARTICIPATING;
        this.joinAt = LocalDateTime.now();
    }

    public void leave(){
        if (this.participantsStatus != ParticipantsStatus.PARTICIPATING) {
            throw new IllegalStateException("참여 중인 참가자만 탈퇴할 수 있습니다.");
        }
        this.participantsStatus = ParticipantsStatus.LEAVED;
        this.leftAt = LocalDateTime.now();
    }

    public void drop(){
        switch (this.participantsStatus) {
            case PARTICIPATING-> {
                this.participantsStatus = ParticipantsStatus.DROPPED;
                this.leftAt = LocalDateTime.now();
            }
            case LEAVED->
                throw new IllegalStateException("이미 탈퇴한 참가자는 강퇴할 수 없습니다.");
            case DROPPED->
                throw new IllegalStateException("이미 강퇴된 참가자입니다.");
        }
    }
}
