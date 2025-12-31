package com.back.domain.report.entity;

/**
 * 신고 처리 상태
 * PENDING: 처리 대기
 * REVIEWING: 검토 중
 * APPROVED: 승인 (제재 조치)
 * REJECTED: 기각
 */
public enum ReportStatus {
    PENDING,    // 처리 대기
    REVIEWING,  // 검토 중
    APPROVED,   // 승인 (제재 조치)
    REJECTED    // 기각
}
