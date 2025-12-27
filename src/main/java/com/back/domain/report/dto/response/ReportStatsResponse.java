package com.back.domain.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 통계 응답 DTO (관리자 대시보드용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatsResponse {

    /**
     * 전체 처리 대기 중인 신고 개수
     */
    private Long totalPending;

    /**
     * 피드 신고 대기 개수
     */
    private Long feedPending;

    /**
     * 댓글 신고 대기 개수
     */
    private Long commentPending;

    /**
     * 함께하기 신고 대기 개수
     */
    private Long togetherPending;
}
