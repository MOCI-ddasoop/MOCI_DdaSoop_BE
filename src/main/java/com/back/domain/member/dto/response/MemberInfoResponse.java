package com.back.domain.member.dto.response;

import com.back.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberInfoResponse {

    private Long memberId;
    private String name;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private String role;
    private String lastLoginProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberInfoResponse from(Member member) {
        return MemberInfoResponse.builder()
            .memberId(member.getId())
            .name(member.getName())
            .nickname(member.getNickname())
            .email(member.getEmail())
            .profileImageUrl(member.getProfileImageUrl())
            .role(member.getRole().name())
            .lastLoginProvider(
                member.getLastLoginProvider() != null 
                    ? member.getLastLoginProvider().name() 
                    : null
            )
            .createdAt(member.getCreatedAt())
            .updatedAt(member.getUpdatedAt())
            .build();
    }
}

