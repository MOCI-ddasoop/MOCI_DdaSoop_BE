package com.back.domain.member.dto.response;

import com.back.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long memberId;
    private String name;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private String lastLoginProvider;

    public static LoginResponse from(Member member) {
        return LoginResponse.builder()
            .memberId(member.getId())
            .name(member.getName())
            .nickname(member.getNickname())
            .email(member.getEmail())
            .profileImageUrl(member.getProfileImageUrl())
            .lastLoginProvider(
                member.getLastLoginProvider() != null 
                    ? member.getLastLoginProvider().name() 
                    : null
            )
            .build();
    }
}

