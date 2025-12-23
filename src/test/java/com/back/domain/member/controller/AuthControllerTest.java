package com.back.domain.member.controller;

import com.back.domain.member.dto.response.LoginResponse;
import com.back.domain.member.service.AuthService;
import com.back.global.exception.GlobalExceptionHandler;
import com.back.global.util.CookieUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private AuthService authService;

    @SuppressWarnings("removal")
    @MockBean
    private CookieUtil cookieUtil;

    @Test
    @DisplayName("1. 로그인 성공")
    void login_success() throws Exception {
        // given
        Long memberId = 1L;
        LoginResponse loginResponse = LoginResponse.builder()
                .memberId(1L)
                .name("홍길동")
                .nickname("hong123")
                .email("hong@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .lastLoginProvider("KAKAO")
                .build();

        Mockito.when(authService.login(Mockito.eq(memberId), Mockito.any(HttpServletResponse.class)))
                .thenReturn(loginResponse);

        // when & then
        mockMvc.perform(
                        post("/api/auth/login")
                                .param("memberId", String.valueOf(memberId))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.nickname").value("hong123"))
                .andExpect(jsonPath("$.email").value("hong@example.com"))
                .andExpect(jsonPath("$.lastLoginProvider").value("KAKAO"));
    }

    @Test
    @DisplayName("2. 로그인 실패 - 회원을 찾을 수 없음")
    void login_fail_memberNotFound() throws Exception {
        // given
        Long memberId = 999L;

        Mockito.when(authService.login(Mockito.eq(memberId), Mockito.any(HttpServletResponse.class)))
                .thenThrow(new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(
                        post("/api/auth/login")
                                .param("memberId", String.valueOf(memberId))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."));
    }

    // 로그아웃 테스트는 @AuthenticationPrincipal이 필요하므로 Security 설정이 필요함
    // 현재 SecurityAutoConfiguration을 제외하고 있어서 테스트 불가
    // TODO: Security 설정 후 테스트 추가

    @Test
    @DisplayName("3. Access Token 갱신 성공")
    void refreshAccessToken_success() throws Exception {
        // given
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";

        Mockito.when(cookieUtil.getRefreshTokenFromCookie(Mockito.any()))
                .thenReturn(refreshToken);
        Mockito.when(authService.refreshAccessToken(refreshToken))
                .thenReturn(newAccessToken);

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "Bearer " + newAccessToken))
                .andExpect(jsonPath("$.accessToken").value(newAccessToken));
    }

    @Test
    @DisplayName("4. Access Token 갱신 실패 - Refresh Token이 없음")
    void refreshAccessToken_fail_noRefreshToken() throws Exception {
        // given
        Mockito.when(cookieUtil.getRefreshTokenFromCookie(Mockito.any()))
                .thenReturn(null);

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
    }

    @Test
    @DisplayName("5. Access Token 갱신 실패 - 유효하지 않은 Refresh Token")
    void refreshAccessToken_fail_invalidRefreshToken() throws Exception {
        // given
        String invalidRefreshToken = "invalid-refresh-token";

        Mockito.when(cookieUtil.getRefreshTokenFromCookie(Mockito.any()))
                .thenReturn(invalidRefreshToken);
        Mockito.when(authService.refreshAccessToken(invalidRefreshToken))
                .thenThrow(new IllegalArgumentException("유효하지 않은 토큰입니다."));

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new jakarta.servlet.http.Cookie("refreshToken", invalidRefreshToken))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
    }

    @Test
    @DisplayName("6. 최근 로그인 방식 조회 성공 - 쿠키에 값이 있는 경우")
    void getLastLoginProvider_success_withCookie() throws Exception {
        // given
        String provider = "KAKAO";

        Mockito.when(cookieUtil.getLastLoginProviderFromCookie(Mockito.any()))
                .thenReturn(provider);

        // when & then
        mockMvc.perform(
                        get("/api/auth/last-login-provider")
                                .cookie(new jakarta.servlet.http.Cookie("lastLoginProvider", provider))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("KAKAO"))
                .andExpect(jsonPath("$.message").value("최근 카카오 계정으로 로그인했습니다."));
    }

    @Test
    @DisplayName("7. 최근 로그인 방식 조회 성공 - 쿠키에 값이 없는 경우")
    void getLastLoginProvider_success_empty() throws Exception {
        // given
        Mockito.when(cookieUtil.getLastLoginProviderFromCookie(Mockito.any()))
                .thenReturn(null);

        // when & then
        mockMvc.perform(
                        get("/api/auth/last-login-provider")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").isEmpty())
                .andExpect(jsonPath("$.message").isEmpty());
    }

    @Test
    @DisplayName("8. 최근 로그인 방식 조회 - 구글")
    void getLastLoginProvider_google() throws Exception {
        // given
        String provider = "GOOGLE";

        Mockito.when(cookieUtil.getLastLoginProviderFromCookie(Mockito.any()))
                .thenReturn(provider);

        // when & then
        mockMvc.perform(
                        get("/api/auth/last-login-provider")
                                .cookie(new jakarta.servlet.http.Cookie("lastLoginProvider", provider))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.message").value("최근 구글 계정으로 로그인했습니다."));
    }

    @Test
    @DisplayName("9. 최근 로그인 방식 조회 - 네이버")
    void getLastLoginProvider_naver() throws Exception {
        // given
        String provider = "NAVER";

        Mockito.when(cookieUtil.getLastLoginProviderFromCookie(Mockito.any()))
                .thenReturn(provider);

        // when & then
        mockMvc.perform(
                        get("/api/auth/last-login-provider")
                                .cookie(new jakarta.servlet.http.Cookie("lastLoginProvider", provider))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("NAVER"))
                .andExpect(jsonPath("$.message").value("최근 네이버 계정으로 로그인했습니다."));
    }
}

