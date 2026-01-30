package com.back.domain.member.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token 엔티티
 *
 * Refresh Token의 해시값을 저장하여 관리합니다.
 * 실제 토큰은 DB에 저장하지 않고 해시값만 저장하여 보안을 강화합니다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    /** Refresh Token을 소유한 회원 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** Refresh Token의 해시값 (실제 토큰은 저장하지 않음) */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    /** 토큰 만료 시각 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 토큰 무효화 여부 (true면 사용 불가) */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    // ========== 비즈니스 로직 메서드 ==========

    /** 토큰 무효화 */
    public void revoke() {
        this.revoked = true;
    }

    /** 토큰이 유효한지 확인 (만료되지 않았고 무효화되지 않음) */
    public boolean isValid() {
        return !revoked && expiresAt.isAfter(LocalDateTime.now());
    }

    /** 토큰이 만료되었는지 확인 */
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}