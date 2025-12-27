package com.back.domain.report.dto.response;

import com.back.domain.report.entity.Report;
import com.back.domain.report.entity.ReportReasonType;
import com.back.domain.report.entity.ReportStatus;
import com.back.domain.report.entity.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고 요약 응답 DTO (목록 조회용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {

    // ========== 신고 기본 정보 ==========
    
    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReasonType reasonType;
    private ReportStatus status;

    // ========== 신고자/피신고자 닉네임만 ==========
    
    private String reporterNickname;
    private String reportedMemberNickname;

    // ========== 시간 정보 ==========
    
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    // ========== 정적 팩토리 메서드 ==========

    /**
     * Report 엔티티 → ReportSummaryResponse 변환
     * 목록 조회 시 사용 (상세 정보 제외)
     */
    public static ReportSummaryResponse from(Report report) {
        return ReportSummaryResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reasonType(report.getReasonType())
                .status(report.getStatus())
                .reporterNickname(report.getReporter().getNickname())
                .reportedMemberNickname(report.getReportedMember() != null ? 
                        report.getReportedMember().getNickname() : null)
                .createdAt(report.getCreatedAt())
                .processedAt(report.getProcessedAt())
                .build();
    }
}
