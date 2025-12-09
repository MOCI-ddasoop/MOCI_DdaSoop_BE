package com.back.domain.member.dto.member.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 로그인 요청 DTO
 * 
 * 카카오 로그인 콜백에서 받은 정보를 담는 DTO입니다.
 * 
 * 사용 예시:
 * - 카카오에서 인증 코드를 받아서 사용자 정보를 가져온 후
 * - 이 DTO에 담아서 서버에 전송
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequest {

    /**
     * 카카오에서 발급한 사용자 고유 ID
     * 예: "1234567890"
     * 
     * 필수: 이 ID로 기존 회원인지 신규 회원인지 판단합니다.
     */
    @NotBlank(message = "카카오 사용자 ID는 필수입니다.")
    private String providerId;

    /**
     * 카카오 계정 이메일
     * 예: "user@example.com"
     * 
     * 선택: 카카오 계정에 이메일이 없을 수 있습니다.
     */
    private String email;

    /**
     * 카카오 계정 이름
     * 예: "홍길동"
     * 
     * 선택: 카카오 계정에 이름이 없을 수 있습니다.
     */
    private String name;

    /**
     * 카카오 프로필 이미지 URL
     * 예: "https://k.kakaocdn.net/..."
     * 
     * 선택: 프로필 이미지가 없을 수 있습니다.
     */
    private String profileImageUrl;
}

