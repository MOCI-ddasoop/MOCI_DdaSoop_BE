package com.back.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

/** JWT 토큰 생성 및 검증 Provider */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String ROLE_CLAIM = "role";
    private static final String TEMPORARY_TOKEN_TYPE = "temporary";
    private final long temporaryTokenValidityInMilliseconds = 30 * 60 * 1000; // 30 minutes

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidityInMilliseconds,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidityInMilliseconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
    }

    /** Access Token 생성 */
    public String createAccessToken(Long memberId, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .claim(MEMBER_ID_CLAIM, memberId)
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /** Refresh Token 생성 */
    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .claim(MEMBER_ID_CLAIM, memberId)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /** 토큰 검증 */
    public boolean validate(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
            return false;
        } catch (JwtException e) {
            log.warn("JWT 토큰 검증에 실패했습니다: {}", e.getMessage());
            return false;
        }
    }

    /** 토큰에서 Authentication 객체 추출 */
    public Authentication getAuthentication(String token) {
        Claims payload = getPayload(token);
        String role = payload.get(ROLE_CLAIM, String.class);
        
        return new UsernamePasswordAuthenticationToken(
                getMemberId(token),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    /** 토큰에서 회원 ID 추출 */
    public Long getMemberId(String token) {
        Claims payload = getPayload(token);
        return payload.get(MEMBER_ID_CLAIM, Long.class);
    }

    /** 토큰에서 역할 추출 */
    public String getRole(String token) {
        Claims payload = getPayload(token);
        return payload.get(ROLE_CLAIM, String.class);
    }

    /** 토큰에서 만료 시간 추출 */
    public Date getExpiration(String token) {
        Claims payload = getPayload(token);
        return payload.getExpiration();
    }

    /** 토큰에서 Payload 추출 */
    private Claims getPayload(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 토큰에서 JwtTokenInfo DTO 추출 */
    public JwtTokenInfo getTokenInfo(String token) {
        Claims payload = getPayload(token);
        return JwtTokenInfo.builder()
                .memberId(payload.get(MEMBER_ID_CLAIM, Long.class))
                .role(payload.get(ROLE_CLAIM, String.class))
                .build();
    }

    /** 임시 토큰 생성 (추가 정보 입력용, 30분 유효) */
    public String createTemporaryToken(Long memberId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + temporaryTokenValidityInMilliseconds);

        return Jwts.builder()
                .claim(MEMBER_ID_CLAIM, memberId)
                .claim("type", TEMPORARY_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /** 임시 토큰 검증 및 회원 ID 추출 */
    public Long validateAndGetMemberIdFromTemporaryToken(String token) {
        // 토큰 유효성 검증
        if (!validate(token)) {
            throw new IllegalArgumentException("유효하지 않은 임시 토큰입니다.");
        }

        Claims payload = getPayload(token);
        
        // 토큰 타입 확인
        String tokenType = payload.get("type", String.class);
        if (!TEMPORARY_TOKEN_TYPE.equals(tokenType)) {
            throw new IllegalArgumentException("유효하지 않은 임시 토큰 타입입니다.");
        }

        // memberId 추출
        Long memberId = payload.get(MEMBER_ID_CLAIM, Long.class);
        if (memberId == null) {
            throw new IllegalArgumentException("임시 토큰에서 회원 ID를 찾을 수 없습니다.");
        }

        return memberId;
    }
}

