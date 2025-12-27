package com.back.domain.report.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 신고 엔티티
 * 피드, 댓글, 함께하기 모임 등에 대한 신고 정보를 저장
 */
@Entity
@Table(name = "reports")
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Report extends BaseEntity {
    
    /** 신고한 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    
    /** 신고 대상 타입 (FEED, COMMENT, TOGETHER) */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    /** 신고 대상 ID (Feed ID, Comment ID, Together ID) */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    
    /** 
     * 신고당한 사람 (신고 대상 콘텐츠의 작성자)
     * 통계 및 제재 이력 관리에 사용
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_member_id")
    private Member reportedMember;

    
    /** 신고 사유 타입 (스팸, 혐오발언, 기타 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 30)
    private ReportReasonType reasonType;

    /** 
     * 상세 사유 (기타 선택 시 필수)
     * 최대 1000자
     */
    @Column(name = "reason_detail", length = 1000)
    private String reasonDetail;

    // ========== 처리 상태 ==========
    
    /** 
     * 처리 상태 (PENDING, REVIEWING, APPROVED, REJECTED)
     * 기본값: PENDING
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    /** 
     * 관리자 코멘트 (처리 결과 메모)
     * 최대 1000자
     */
    @Column(name = "admin_comment", length = 1000)
    private String adminComment;

    /** 처리 완료 시간 */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** 처리한 관리자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private Member processedBy;




    /**
     * 신고 처리 (관리자)
     * 
     * @param admin 처리하는 관리자
     * @param newStatus 새로운 상태 (APPROVED 또는 REJECTED)
     * @param comment 관리자 코멘트
     */
    public void process(Member admin, ReportStatus newStatus, String comment) {
        validateProcessStatus(newStatus);
        
        this.status = newStatus;
        this.adminComment = comment;
        this.processedAt = LocalDateTime.now();
        this.processedBy = admin;
    }

    /**
     * 신고 승인 (제재 조치)
     * 
     * @param admin 처리하는 관리자
     * @param comment 관리자 코멘트
     */
    public void approve(Member admin, String comment) {
        process(admin, ReportStatus.APPROVED, comment);
    }

    /**
     * 신고 기각
     * 
     * @param admin 처리하는 관리자
     * @param comment 관리자 코멘트
     */
    public void reject(Member admin, String comment) {
        process(admin, ReportStatus.REJECTED, comment);
    }

    /**
     * 검토 중 상태로 변경
     * 
     * @param admin 검토 시작한 관리자
     */
    public void startReview(Member admin) {
        if (this.status != ReportStatus.PENDING) {
            throw new IllegalStateException("대기 중인 신고만 검토를 시작할 수 있습니다.");
        }
        
        this.status = ReportStatus.REVIEWING;
        this.processedBy = admin;
    }

    /**
     * 처리 대기 중인지 확인
     */
    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }

    /**
     * 처리 완료되었는지 확인 (승인 또는 기각)
     */
    public boolean isProcessed() {
        return this.status == ReportStatus.APPROVED || this.status == ReportStatus.REJECTED;
    }

    /**
     * 신고가 승인되었는지 확인
     */
    public boolean isApproved() {
        return this.status == ReportStatus.APPROVED;
    }

    /**
     * 신고가 기각되었는지 확인
     */
    public boolean isRejected() {
        return this.status == ReportStatus.REJECTED;
    }

    // ========== Private 헬퍼 메서드 ==========

    /**
     * 처리 상태 검증
     * APPROVED 또는 REJECTED만 허용
     */
    private void validateProcessStatus(ReportStatus newStatus) {
        if (newStatus != ReportStatus.APPROVED && newStatus != ReportStatus.REJECTED) {
            throw new IllegalArgumentException(
                "신고 처리는 APPROVED 또는 REJECTED만 가능합니다. 입력값: " + newStatus
            );
        }
        
        if (this.status == ReportStatus.APPROVED || this.status == ReportStatus.REJECTED) {
            throw new IllegalStateException(
                "이미 처리 완료된 신고입니다. 현재 상태: " + this.status
            );
        }
    }

    /**
     * 동일 신고인지 확인 (중복 신고 방지용)
     * 
     * @param reporterId 신고자 ID
     * @param targetType 대상 타입
     * @param targetId 대상 ID
     * @return 동일 신고 여부
     */
    public boolean isSameReport(Long reporterId, ReportTargetType targetType, Long targetId) {
        return this.reporter.getId().equals(reporterId)
                && this.targetType == targetType
                && this.targetId.equals(targetId);
    }
}
