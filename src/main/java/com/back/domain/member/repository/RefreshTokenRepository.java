package com.back.domain.member.repository;

import com.back.domain.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** RefreshToken 엔티티를 위한 Repository 인터페이스 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // ========== 기본 조회 ==========

    /** 토큰 해시값으로 Refresh Token 조회 */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 회원 ID로 Refresh Token 조회 */
    Optional<RefreshToken> findByMemberId(Long memberId);

    // ========== 삭제 ==========

    /** 회원 ID로 모든 Refresh Token 삭제 */
    void deleteByMemberId(Long memberId);

    /** 토큰 해시값으로 Refresh Token 삭제 */
    void deleteByTokenHash(String tokenHash);

    // ========== 존재 여부 확인 ==========

    /** 회원 ID로 Refresh Token 존재 여부 확인 */
    boolean existsByMemberId(Long memberId);
}

