package com.back.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/** 쿠키 관련 유틸리티 (Refresh Token 쿠키 관리) */
@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
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
}

