package com.back.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberWithdrawResponse {

    private String message;

    public static MemberWithdrawResponse success() {
        return MemberWithdrawResponse.builder()
            .message("회원 탈퇴가 완료되었습니다.")
            .build();
    }
}

