package com.back.domain.member.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 회원의 소셜 로그인 계정 정보 (하나의 Member는 여러 소셜 계정 보유 가능) */
@Entity
@Table(
    name = "member_social_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_provider_provider_id",
            columnNames = {"provider", "provider_id"}
        )
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSocialAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 마지막 로그인 시간 업데이트 */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}

