package com.back.domain.member.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.repository.RedisTokenRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.jwt.JwtTokenProvider;
import com.back.global.util.CookieUtil;
import com.back.global.util.TokenHashUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RedisTokenRepository redisTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityInMilliseconds;

    public String login(Long memberId, HttpServletResponse response) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        redisTokenRepository.deleteByMemberId(memberId);

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        String tokenHash = TokenHashUtil.hash(refreshToken);

        int ttlSeconds = (int) (refreshTokenValidityInMilliseconds / 1000);
        redisTokenRepository.save(tokenHash, memberId, ttlSeconds);

        cookieUtil.setRefreshTokenCookie(response, refreshToken);
        return accessToken;
    }

    public void logout(Long memberId, HttpServletResponse response) {
        redisTokenRepository.deleteByMemberId(memberId);
        cookieUtil.deleteRefreshTokenCookie(response);
    }

    public String refreshAccessToken(String refreshTokenString) {
        if (!jwtTokenProvider.validate(refreshTokenString)) {
            throw new IllegalArgumentException(ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }

        String tokenHash = TokenHashUtil.hash(refreshTokenString);
        Long memberId = redisTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND.getMessage()));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        return jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
    }
}

