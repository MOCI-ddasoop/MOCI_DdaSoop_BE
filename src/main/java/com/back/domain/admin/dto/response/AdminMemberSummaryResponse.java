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
public class AdminMemberSummaryResponse {

    private Long id;
    private String nickname;
    private String email;
    private String role;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;

    public static AdminMemberSummaryResponse from(Member member) {
        return AdminMemberSummaryResponse.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .role(member.getRole().name())
                .deletedAt(member.getDeletedAt())
                .createdAt(member.getCreatedAt())
                .build();
    }

    public boolean getIsDeleted() {
        return deletedAt != null;
    }
}
