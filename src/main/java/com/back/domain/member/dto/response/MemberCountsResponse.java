package com.back.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCountsResponse {

    private Long likedCount;      // 좋아요 개수
    private Long commentedCount;  // 댓글 개수
    private Long feedCount;       // 피드 개수
}
