package com.back.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 대시보드 통계 응답 DTO.
 * 회원/피드/댓글/함께하기/기부/신고 집계만 포함 (다른 도메인 엔티티 미참조).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private Long memberCount;
    private Long feedCount;
    private Long commentCount;
    private Long togetherCount;
    private Long donationCount;

    private Long reportPendingTotal;
    private Long reportPendingFeed;
    private Long reportPendingComment;
    private Long reportPendingTogether;
}
