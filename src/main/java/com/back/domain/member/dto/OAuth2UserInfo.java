package com.back.domain.member.dto;

import com.back.domain.member.entity.SocialProvider;
import lombok.Builder;
import lombok.Getter;

/**
 * OAuth2 소셜 로그인에서 받아온 사용자 정보를 담는 DTO
 * 
 * 각 소셜 로그인 제공자(Google, Kakao, Naver)의 응답 형식이 다르므로,
 * 이를 통일된 형식으로 변환하여 사용합니다.
 */
@Getter
@Builder
public class OAuth2UserInfo {
    
    /** 소셜 로그인 제공자 */
    private SocialProvider provider;
    
    /** 소셜 로그인 제공자에서 발급한 고유 ID */
    private String providerId;
    
    /** 사용자 이름 */
    private String name;
    
    /** 이메일 */
    private String email;
    
    /** 프로필 이미지 URL */
    private String profileImageUrl;
    
    /** 닉네임 (없으면 name 사용) */
    private String nickname;
}

