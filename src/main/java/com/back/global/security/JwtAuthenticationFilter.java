package com.back.global.security;

import com.back.domain.member.service.AuthService;
import com.back.global.jwt.JwtTokenProvider;
import com.back.global.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** JWT 토큰 검증 필터 (SecurityContext에 인증 정보 설정). AT 만료 시 RT로 자동 갱신 시도 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final AuthService authService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validate(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 토큰 인증 성공 - Member ID: {}", jwtTokenProvider.getMemberId(token));
        } else {
            tryAutoRefresh(request, response, token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * AT가 없거나 만료된 경우, Refresh 엔드포인트가 아니면 쿠키의 RT로 갱신 시도.
     * 성공 시 SecurityContext 설정 및 응답 헤더에 새 AT 추가.
     */
    private void tryAutoRefresh(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        if (REFRESH_PATH.equals(request.getRequestURI())) {
            log.debug("Refresh 엔드포인트 요청이므로 자동 갱신 스킵");
            return;
        }
        if (!StringUtils.hasText(accessToken)) {
            log.debug("AT가 없어 자동 갱신 시도하지 않음");
            return;
        }

        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            log.debug("RT 쿠키 없음");
            return;
        }

        try {
            String newAccessToken = authService.refreshAccessToken(refreshToken);
            Authentication authentication = jwtTokenProvider.getAuthentication(newAccessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            response.setHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + newAccessToken);
            log.debug("AT 만료로 자동 갱신 성공 - Member ID: {}", jwtTokenProvider.getMemberId(newAccessToken));
        } catch (Exception e) {
            log.debug("자동 갱신 실패: {}", e.getMessage());
        }
    }

    /** 요청 헤더에서 JWT 토큰 추출 */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

