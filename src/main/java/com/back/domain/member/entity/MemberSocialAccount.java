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
 * 
 * 하나의 Member는 여러 소셜 계정을 가질 수 있습니다.
 * 예: Google로 가입 → 나중에 Kakao 계정도 연결
 * 
 * 사용 예시:
 * - Google로 로그인한 회원: provider=GOOGLE, providerId="google_user_123"
 * - Kakao로 로그인한 회원: provider=KAKAO, providerId="kakao_user_456"
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

    /**
     * 소셜 계정을 소유한 회원
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    /**
     * 소셜 로그인 제공자에서 발급한 고유 ID
     * 예: Google의 경우 "google_user_123456789"
     * 
     * provider와 providerId의 조합은 유일해야 합니다.
     * (같은 Google 계정이 여러 Member에 연결되는 것을 방지)
     */
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    /**
     * 이 소셜 계정으로 마지막 로그인한 시각
     * 최근 로그인 방식을 추적하기 위해 사용됩니다.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ========== 비즈니스 로직 메서드 ==========

    /**
     * 소셜 계정으로 로그인 시 호출
     * lastLoginAt을 현재 시간으로 업데이트합니다.
     * 
     * 사용 예시:
     * MemberSocialAccount account = memberSocialAccountRepository.findByProviderAndProviderId(...);
     * account.updateLastLogin();  // lastLoginAt = 현재 시간
     * memberSocialAccountRepository.save(account);
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}

