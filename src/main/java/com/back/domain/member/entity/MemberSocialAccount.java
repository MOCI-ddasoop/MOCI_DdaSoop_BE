package com.back.domain.member.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원의 소셜 로그인 계정 정보
 * 하나의 Member는 여러 소셜 계정을 가질 수 있음
 */
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

    /** 소셜 계정을 소유한 회원 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    /** 소셜 로그인 제공자에서 발급한 고유 ID (provider와 providerId 조합은 유일) */
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    /** 이 소셜 계정으로 마지막 로그인한 시각 */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ========== 비즈니스 로직 메서드 ==========

    /** 소셜 계정으로 로그인 시 lastLoginAt을 현재 시간으로 업데이트 */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}

