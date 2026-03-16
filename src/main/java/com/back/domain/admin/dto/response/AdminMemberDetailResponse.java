package com.back.domain.admin.dto.response;

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
public class AdminMemberDetailResponse {

    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String memberCode;
    private String profileImageUrl;
    private String role;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminMemberDetailResponse from(Member member) {
        return AdminMemberDetailResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .memberCode(member.getMemberCode())
                .profileImageUrl(member.getProfileImageUrl())
                .role(member.getRole().name())
                .deletedAt(member.getDeletedAt())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}
