package com.back.global.security;

import com.back.domain.member.dto.OAuth2UserInfo;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.SocialProvider;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthService;
import com.back.domain.member.service.SocialLoginService;
import com.back.domain.member.util.OAuth2UserInfoFactory;
import com.back.global.exception.ErrorCode;
import com.back.global.jwt.JwtTokenProvider;
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
import java.net.URI;

/** OAuth2 소셜 로그인 성공 핸들러 (회원 조회/생성, JWT 토큰 발급) */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

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

            // 최근 로그인 방식을 쿠키에 저장
            cookieUtil.setLastLoginProviderCookie(response, provider.name());

            // 트랜잭션 커밋 후 최신 데이터를 보장하기 위해 다시 조회
            // (findOrCreateMember의 트랜잭션이 커밋된 후 최신 데이터 확인)
            // 소프트 딜리트된 회원도 조회 가능하도록 findById() 사용
            Member latestMember = memberRepository.findById(member.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            ErrorCode.MEMBER_NOT_FOUND.getMessage()
                    ));
            
            // 탈퇴한 회원인지 확인 (소프트 딜리트 체크)
            if (latestMember.isDeleted()) {
                throw new IllegalArgumentException(
                        ErrorCode.MEMBER_ALREADY_DELETED.getMessage()
                );
            }

            // 추가 정보 입력 필요 여부 확인 (최신 데이터 기준)
            String targetUrl;
            if (latestMember.isAdditionalInfoRequired()) {
                // 추가 정보 입력이 필요한 경우: 임시 토큰 발급 (회원 상태 미확정)
                // OAuth 인증만 완료하고, JWT는 추가 정보 입력 완료 후에만 발급
                String temporaryToken = jwtTokenProvider.createTemporaryToken(latestMember.getId());
                targetUrl = UriComponentsBuilder.fromUriString(getFrontendOrigin() + "/login-additional")
                        .queryParam("token", temporaryToken)
                        .queryParam("provider", provider.name())
                        .build()
                        .toUriString();
            } else {
                // 추가 정보 입력이 완료된 경우: JWT 발급 (회원 상태 확정)
                authService.login(latestMember.getId(), response);
                targetUrl = UriComponentsBuilder.fromUriString(getFrontendOrigin())
                        .queryParam("provider", provider.name())
                        .build()
                        .toUriString();
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

    /**
     * app.oauth2.redirect-uri 값에서 프론트 origin(scheme://host[:port])을 추출한다.
     * 예: https://ddasoop.xyz/auth/callback -> https://ddasoop.xyz
     */
    private String getFrontendOrigin() {
        try {
            URI uri = URI.create(redirectUri);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            if (scheme == null || host == null) {
                return "http://localhost:3000";
            }

            if (port == -1) {
                return scheme + "://" + host;
            }

            return scheme + "://" + host + ":" + port;
        } catch (Exception e) {
            log.warn("redirect-uri 파싱 실패, localhost fallback 사용: {}", redirectUri);
            return "http://localhost:3000";
        }
    }

}

