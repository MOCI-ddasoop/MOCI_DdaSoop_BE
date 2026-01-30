package com.back.domain.member.entity;

/**
 * 소셜 로그인 제공자 타입
 * 
 * 사용 예시:
 * - Google 로그인: SocialProvider.GOOGLE
 * - Kakao 로그인: SocialProvider.KAKAO
 * - Naver 로그인: SocialProvider.NAVER
 */
public enum SocialProvider {
    GOOGLE("구글"),
    KAKAO("카카오"),
    NAVER("네이버");

    private final String displayName;

    SocialProvider(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 사용자에게 보여줄 한글 이름
     * 
     * @return 한글 이름 (예: "구글", "카카오", "네이버")
     */
    public String getDisplayName() {
        return displayName;
    }
}

