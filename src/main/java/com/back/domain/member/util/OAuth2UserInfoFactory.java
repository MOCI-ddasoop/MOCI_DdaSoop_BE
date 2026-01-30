package com.back.domain.member.util;

import com.back.domain.member.dto.OAuth2UserInfo;
import com.back.domain.member.entity.SocialProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/** OAuth2 사용자 정보를 OAuth2UserInfo로 변환 (null 값 허용) */
public class OAuth2UserInfoFactory {

    /** OAuth2User를 OAuth2UserInfo로 변환 */
    public static OAuth2UserInfo of(SocialProvider provider, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        return switch (provider) {
            case GOOGLE -> createGoogleUserInfo(attributes);
            case KAKAO -> createKakaoUserInfo(attributes);
            case NAVER -> createNaverUserInfo(attributes);
        };
    }

    /** Google 사용자 정보 생성 */
    private static OAuth2UserInfo createGoogleUserInfo(Map<String, Object> attributes) {
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String picture = (String) attributes.get("picture");
        
        return OAuth2UserInfo.builder()
                .provider(SocialProvider.GOOGLE)
                .providerId((String) attributes.get("sub"))
                .name(name)
                .email(email)
                .profileImageUrl(picture)
                .nickname(name)
                .build();
    }

    /** Kakao 사용자 정보 생성 */
    @SuppressWarnings("unchecked")
    private static OAuth2UserInfo createKakaoUserInfo(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = attributes.get("kakao_account") != null
                ? (Map<String, Object>) attributes.get("kakao_account")
                : null;
        
        Map<String, Object> profile = kakaoAccount != null && kakaoAccount.get("profile") != null
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : null;

        String providerId = String.valueOf(attributes.get("id"));
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImageUrl = profile != null ? (String) profile.get("profile_image_url") : null;
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        return OAuth2UserInfo.builder()
                .provider(SocialProvider.KAKAO)
                .providerId(providerId)
                .name(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .nickname(nickname)
                .build();
    }

    /** Naver 사용자 정보 생성 */
    @SuppressWarnings("unchecked")
    private static OAuth2UserInfo createNaverUserInfo(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        if (response == null) {
            throw new IllegalArgumentException("Naver 응답 형식이 올바르지 않습니다.");
        }

        String name = (String) response.get("name");
        String email = (String) response.get("email");
        String profileImage = (String) response.get("profile_image");
        String nickname = (String) response.get("nickname");

        return OAuth2UserInfo.builder()
                .provider(SocialProvider.NAVER)
                .providerId((String) response.get("id"))
                .name(name)
                .email(email)
                .profileImageUrl(profileImage)
                .nickname(nickname != null ? nickname : name)
                .build();
    }
}

