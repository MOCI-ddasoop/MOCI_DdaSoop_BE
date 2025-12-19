package com.back.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/** 쿠키 관련 유틸리티 (Refresh Token 쿠키 관리) */
@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String LAST_LOGIN_PROVIDER_COOKIE_NAME = "lastLoginProvider";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    private static final int LAST_LOGIN_PROVIDER_COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30일
    private static final boolean HTTP_ONLY = true;
    private static final boolean SECURE = false;

    /** Refresh Token을 쿠키에 저장 */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath("/");
        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);
        response.addCookie(cookie);
    }

    /** Refresh Token 쿠키 삭제 */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
        Cookie cookie = new Cookie(LAST_LOGIN_PROVIDER_COOKIE_NAME, provider);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath("/");
        cookie.setMaxAge(LAST_LOGIN_PROVIDER_COOKIE_MAX_AGE);
        response.addCookie(cookie);
    }

    /** 최근 로그인 방식 쿠키 삭제 */
    public void deleteLastLoginProviderCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(LAST_LOGIN_PROVIDER_COOKIE_NAME, null);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
}

