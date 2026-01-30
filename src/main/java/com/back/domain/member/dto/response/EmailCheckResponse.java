package com.back.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCheckResponse {

    private boolean available;
    private String message;

    public static EmailCheckResponse available(String email) {
        return EmailCheckResponse.builder()
            .available(true)
            .message("사용 가능한 이메일입니다.")
            .build();
    }

    public static EmailCheckResponse unavailable(String email) {
        return EmailCheckResponse.builder()
            .available(false)
            .message("이미 사용 중인 이메일입니다.")
            .build();
    }
}

