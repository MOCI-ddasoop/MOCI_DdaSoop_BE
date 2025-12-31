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
 * 신고 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    // ========== 신고 기본 정보 ==========
    
    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReasonType reasonType;
    private String reasonDetail;
    private ReportStatus status;

    // ========== 신고자 정보 ==========
    
    private Long reporterId;
    private String reporterNickname;

    // ========== 피신고자 정보 ==========
    
    private Long reportedMemberId;
    private String reportedMemberNickname;

    // ========== 처리 정보 (관리자용) ==========
    
    private String adminComment;
    private LocalDateTime processedAt;
    private Long processedById;
    private String processedByNickname;

    // ========== 시간 정보 ==========
    
    private LocalDateTime createdAt;

    // ========== 정적 팩토리 메서드 ==========

    /**
     * Report 엔티티 → ReportResponse 변환
     */
    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reasonType(report.getReasonType())
                .reasonDetail(report.getReasonDetail())
                .status(report.getStatus())
                .reporterId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickname())
                .reportedMemberId(report.getReportedMember() != null ? 
                        report.getReportedMember().getId() : null)
                .reportedMemberNickname(report.getReportedMember() != null ? 
                        report.getReportedMember().getNickname() : null)
                .adminComment(report.getAdminComment())
                .processedAt(report.getProcessedAt())
                .processedById(report.getProcessedBy() != null ? 
                        report.getProcessedBy().getId() : null)
                .processedByNickname(report.getProcessedBy() != null ? 
                        report.getProcessedBy().getNickname() : null)
                .createdAt(report.getCreatedAt())
                .build();
    }
}
