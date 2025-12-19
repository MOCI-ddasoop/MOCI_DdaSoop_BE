package com.back.domain.member.service;

import com.back.domain.member.dto.response.LoginResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.RefreshToken;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.repository.RefreshTokenRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.jwt.JwtTokenProvider;
import com.back.global.util.CookieUtil;
import com.back.global.util.TokenHashUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/** 인증 서비스 (로그인, 로그아웃, 토큰 갱신) */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    /** 로그인 처리 (JWT 토큰 발급) */
    @Transactional
    public LoginResponse login(Long memberId, HttpServletResponse response) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage()
                ));

        refreshTokenRepository.findByMemberId(memberId).ifPresent(RefreshToken::revoke);

        jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        String tokenHash = TokenHashUtil.hash(refreshToken);
        LocalDateTime expiresAt = calculateExpiresAt(refreshToken);

        refreshTokenRepository.save(RefreshToken.builder()
                .member(member)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build());

        cookieUtil.setRefreshTokenCookie(response, refreshToken);
        log.info("로그인 성공 - Member ID: {}, Email: {}", 
                member.getId(), member.getEmail() != null ? member.getEmail() : "(없음)");

        return LoginResponse.from(member);
    }

    /** 소셜 로그인용 Access Token 발급 */
    @Transactional
    public String loginAndGetAccessToken(Long memberId, HttpServletResponse response) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage()
                ));

        refreshTokenRepository.findByMemberId(memberId).ifPresent(RefreshToken::revoke);

        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        String tokenHash = TokenHashUtil.hash(refreshToken);
        LocalDateTime expiresAt = calculateExpiresAt(refreshToken);

        refreshTokenRepository.save(RefreshToken.builder()
                .member(member)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build());

        cookieUtil.setRefreshTokenCookie(response, refreshToken);
        log.info("소셜 로그인 토큰 발급 - Member ID: {}, Email: {}", 
                member.getId(), member.getEmail() != null ? member.getEmail() : "(없음)");

        return accessToken;
    }

    /** 로그아웃 처리 */
    @Transactional
    public void logout(Long memberId, HttpServletResponse response) {
        refreshTokenRepository.findByMemberId(memberId)
                .ifPresent(refreshToken -> {
                    refreshToken.revoke();
                    refreshTokenRepository.save(refreshToken);
                });
        cookieUtil.deleteRefreshTokenCookie(response);

        log.info("로그아웃 성공 - Member ID: {}", memberId);
    }

    /** Access Token 갱신 */
    @Transactional
    public String refreshAccessToken(String refreshTokenString) {
        if (!jwtTokenProvider.validate(refreshTokenString)) {
            throw new IllegalArgumentException(ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshTokenString);
        String tokenHash = TokenHashUtil.hash(refreshTokenString);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND.getMessage()));

        if (!refreshToken.isValid()) {
            if (refreshToken.isExpired()) {
                throw new IllegalArgumentException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED.getMessage());
            } else {
                throw new IllegalArgumentException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED.getMessage());
            }
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole().name());

        log.info("Access Token 갱신 성공 - Member ID: {}", memberId);

        return newAccessToken;
    }

    /** Refresh Token 만료 시간 계산 */
    private LocalDateTime calculateExpiresAt(String token) {
        try {
            Date expiration = jwtTokenProvider.getExpiration(token);
            return Instant.ofEpochMilli(expiration.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            log.error("토큰 만료 시간 계산 실패", e);
            return LocalDateTime.now().plusDays(7);
        }
    }
}

