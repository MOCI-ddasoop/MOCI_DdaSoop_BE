package com.back.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastLoginProviderResponse {

    private String provider;
    private String message;

    public static LastLoginProviderResponse from(String provider) {
        if (provider == null) {
            return LastLoginProviderResponse.builder()
                .provider(null)
                .message(null)
                .build();
        }

        String displayName = getDisplayName(provider);
        return LastLoginProviderResponse.builder()
            .provider(provider)
            .message("최근 " + displayName + " 계정으로 로그인했습니다.")
            .build();
    }

    public static LastLoginProviderResponse empty() {
        return LastLoginProviderResponse.builder()
            .provider(null)
            .message(null)
            .build();
    }

    private static String getDisplayName(String provider) {
        return switch (provider) {
            case "KAKAO" -> "카카오";
            case "GOOGLE" -> "구글";
            case "NAVER" -> "네이버";
            default -> provider;
        };
    }
}

