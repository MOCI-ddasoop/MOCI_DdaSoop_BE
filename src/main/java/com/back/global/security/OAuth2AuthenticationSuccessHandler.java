package com.back.global.security;

import com.back.domain.member.dto.OAuth2UserInfo;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.SocialProvider;
import com.back.domain.member.service.AuthService;
import com.back.domain.member.service.SocialLoginService;
import com.back.domain.member.util.OAuth2UserInfoFactory;
import com.back.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/** OAuth2 소셜 로그인 성공 핸들러 (회원 조회/생성, JWT 토큰 발급) */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oAuth2Token.getPrincipal();
            String registrationId = oAuth2Token.getAuthorizedClientRegistrationId();
            SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());

            OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(provider, oAuth2User);
            Member member = socialLoginService.findOrCreateMember(userInfo);
            String accessToken = authService.loginAndGetAccessToken(member.getId(), response);

            // 최근 로그인 방식을 쿠키에 저장
            cookieUtil.setLastLoginProviderCookie(response, provider.name());

            // 추가 정보 입력 필요 여부 확인
            String targetUrl;
            if (member.isAdditionalInfoRequired()) {
                // 추가 정보 입력이 필요한 경우 (닉네임 또는 이메일이 없음)
                targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login-additional")
                        .queryParam("accessToken", accessToken)
                        .queryParam("memberId", member.getId())
                        .queryParam("provider", provider.name())
                        .build()
                        .toUriString();
                log.info("소셜 로그인 성공 (추가 정보 입력 필요) - Provider: {}, MemberId: {}", provider, member.getId());
            } else {
                // 추가 정보 입력이 완료된 경우 (기존 회원 또는 정보가 모두 있는 신규 회원)
                targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000")
                        .queryParam("accessToken", accessToken)
                        .queryParam("provider", provider.name())
                        .build()
                        .toUriString();
                log.info("소셜 로그인 성공 - Provider: {}, MemberId: {}", provider, member.getId());
            }

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 오류 발생", e);
            String errorUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "login_failed")
                    .queryParam("message", "소셜 로그인 처리 중 오류가 발생했습니다.")
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

}

