package com.back.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/** 쿠키 관련 유틸리티 (Refresh Token 쿠키 관리) */
@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String LAST_LOGIN_PROVIDER_COOKIE_NAME = "lastLoginProvider";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    private static final int LAST_LOGIN_PROVIDER_COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30일
    private static final boolean HTTP_ONLY = true;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    /** Refresh Token을 쿠키에 저장 */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, REFRESH_TOKEN_COOKIE_MAX_AGE);
    }

    /** Refresh Token 쿠키 삭제 */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, "", 0);
    }

    /** 요청에서 Refresh Token 쿠키 추출 */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** 최근 로그인 방식을 쿠키에 저장 */
    public void setLastLoginProviderCookie(HttpServletResponse response, String provider) {
        addCookie(response, LAST_LOGIN_PROVIDER_COOKIE_NAME, provider, LAST_LOGIN_PROVIDER_COOKIE_MAX_AGE);
    }

    /** 최근 로그인 방식 쿠키 삭제 */
    public void deleteLastLoginProviderCookie(HttpServletResponse response) {
        addCookie(response, LAST_LOGIN_PROVIDER_COOKIE_NAME, "", 0);
    }

    /** 요청에서 최근 로그인 방식 쿠키 추출 */
    public String getLastLoginProviderFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (LAST_LOGIN_PROVIDER_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(HTTP_ONLY)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite(cookieSameSite);

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}

