package com.back.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NicknameCheckResponse {

    private boolean available;
    private String message;

    public static NicknameCheckResponse available(String nickname) {
        return NicknameCheckResponse.builder()
            .available(true)
            .message("사용 가능한 닉네임입니다.")
            .build();
    }

    public static NicknameCheckResponse unavailable(String nickname) {
        return NicknameCheckResponse.builder()
            .available(false)
            .message("이미 사용 중인 닉네임입니다.")
            .build();
    }
}

