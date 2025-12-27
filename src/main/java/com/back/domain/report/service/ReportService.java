package com.back.domain.report.service;

import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.report.entity.Report;
import com.back.domain.report.entity.ReportReasonType;
import com.back.domain.report.entity.ReportStatus;
import com.back.domain.report.entity.ReportTargetType;
import com.back.domain.report.repository.ReportRepository;
import com.back.domain.together.entity.Together;
import com.back.domain.together.repository.TogetherRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final TogetherRepository togetherRepository;

    // ========== 신고 생성 ==========

    /**
     * 신고 생성
     */
    @Transactional
    public Long createReport(
            ReportTargetType targetType,
            Long targetId,
            ReportReasonType reasonType,
            String reasonDetail,
            Long reporterId
    ) {
        // 1. 신고자 조회
        Member reporter = memberRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        // 2. 중복 신고 확인
        boolean alreadyReported = reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, targetType, targetId
        );
        if (alreadyReported) {
            throw new IllegalArgumentException("이미 신고한 콘텐츠입니다.");
        }

        // 3. 신고 대상 존재 여부 및 작성자 확인
        Member reportedMember = getTargetAuthor(targetType, targetId);

        // 4. 자기 자신 신고 방지
        if (reportedMember != null && reportedMember.getId().equals(reporterId)) {
            throw new IllegalArgumentException("자기 자신의 콘텐츠는 신고할 수 없습니다.");
        }

        // 5. 기타 사유 선택 시 상세 사유 필수
        if (reasonType == ReportReasonType.OTHER) {
            if (reasonDetail == null || reasonDetail.isBlank()) {
                throw new IllegalArgumentException("기타 사유 선택 시 상세 사유를 입력해야 합니다.");
            }
        }

        // 6. 신고 생성
        Report report = Report.builder()
                .reporter(reporter)
                .targetType(targetType)
                .targetId(targetId)
                .reportedMember(reportedMember)
                .reasonType(reasonType)
                .reasonDetail(reasonDetail)
                .status(ReportStatus.PENDING)
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("신고 생성 완료 - ID: {}, 대상: {} {}, 사유: {}", 
                savedReport.getId(), targetType, targetId, reasonType);

        // 7. 신고 누적 확인 및 자동 조치
        checkAndApplyAutoAction(targetType, targetId);

        return savedReport.getId();
    }

    // ========== 신고 조회 ==========

    /**
     * 신고 상세 조회
     */
    public Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));
    }

    /**
     * 상태별 신고 목록 조회 (관리자용)
     */
    public Page<Report> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * 타입 + 상태별 신고 목록 조회 (관리자용)
     */
    public Page<Report> getReportsByTypeAndStatus(
            ReportTargetType targetType,
            ReportStatus status,
            Pageable pageable
    ) {
        return reportRepository.findByTargetTypeAndStatusOrderByCreatedAtDesc(
                targetType, status, pageable
        );
    }

    /**
     * 특정 대상의 신고 목록 조회 (관리자용)
     */
    public List<Report> getReportsByTarget(ReportTargetType targetType, Long targetId) {
        return reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                targetType, targetId
        );
    }

    /**
     * 특정 회원이 한 신고 목록 조회 (마이페이지용)
     */
    public Page<Report> getMyReports(Long reporterId, Pageable pageable) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable);
    }

    /**
     * 특정 회원이 받은 신고 목록 조회 (관리자용)
     */
    public Page<Report> getReportsReceivedByMember(Long reportedMemberId, Pageable pageable) {
        return reportRepository.findByReportedMemberIdOrderByCreatedAtDesc(
                reportedMemberId, pageable
        );
    }

    // ========== 신고 처리 (관리자) ==========

    /**
     * 신고 승인 (제재 조치)
     */
    @Transactional
    public void approveReport(Long reportId, Long adminId, String adminComment) {
        Report report = getReport(reportId);
        Member admin = getAdminMember(adminId);

        report.approve(admin, adminComment);

        log.info("신고 승인 완료 - 신고 ID: {}, 관리자: {}, 대상: {} {}", 
                reportId, admin.getNickname(), report.getTargetType(), report.getTargetId());

        // 제재 조치 실행
        executeAction(report.getTargetType(), report.getTargetId());
    }

    /**
     * 신고 기각
     */
    @Transactional
    public void rejectReport(Long reportId, Long adminId, String adminComment) {
        Report report = getReport(reportId);
        Member admin = getAdminMember(adminId);

        report.reject(admin, adminComment);

        log.info("신고 기각 완료 - 신고 ID: {}, 관리자: {}", reportId, admin.getNickname());
    }

    /**
     * 검토 시작
     */
    @Transactional
    public void startReview(Long reportId, Long adminId) {
        Report report = getReport(reportId);
        Member admin = getAdminMember(adminId);

        report.startReview(admin);

        log.info("신고 검토 시작 - 신고 ID: {}, 관리자: {}", reportId, admin.getNickname());
    }

    // ========== 통계 조회 ==========

    /**
     * 처리 대기 중인 신고 개수
     */
    public Long getPendingReportCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    /**
     * 타입별 처리 대기 중인 신고 개수
     */
    public Long getPendingReportCountByType(ReportTargetType targetType) {
        return reportRepository.countByTargetTypeAndStatus(targetType, ReportStatus.PENDING);
    }

    /**
     * 특정 대상의 신고 횟수
     */
    public Long getReportCount(ReportTargetType targetType, Long targetId) {
        return reportRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }

    /**
     * 특정 회원이 받은 신고 횟수
     */
    public Long getReportCountByMember(Long memberId) {
        return reportRepository.countByReportedMemberId(memberId);
    }

    /**
     * 신고 많은 콘텐츠 조회
     */
    public List<Long> getFrequentlyReportedTargets(
            ReportTargetType targetType,
            Long minCount,
            Pageable pageable
    ) {
        return reportRepository.findFrequentlyReportedTargets(targetType, minCount, pageable);
    }

    /**
     * 신고 많이 받은 회원 조회
     */
    public List<Long> getFrequentlyReportedMembers(Long minCount, Pageable pageable) {
        return reportRepository.findFrequentlyReportedMembers(minCount, pageable);
    }

    // ========== Private 헬퍼 메서드 ==========

    /**
     * 신고 대상의 작성자 조회
     */
    private Member getTargetAuthor(ReportTargetType targetType, Long targetId) {
        return switch (targetType) {
            case FEED -> {
                Feed feed = feedRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("신고 대상 피드를 찾을 수 없습니다."));
                yield feed.getMember();
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("신고 대상 댓글을 찾을 수 없습니다."));
                yield comment.getMember();
            }
            case TOGETHER -> {
                Together together = togetherRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("신고 대상 모임을 찾을 수 없습니다."));
                yield together.getMember();
            }
        };
    }

    /**
     * 관리자 회원 조회 및 권한 확인
     * 
     * @param adminId 관리자 ID
     * @return 관리자 Member
     */
    private Member getAdminMember(Long adminId) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        return admin;
    }

    /**
     * 신고 누적 확인 및 자동 조치
     * 10회 이상 신고 시 자동 비공개/삭제
     * 
     * @param targetType 대상 타입
     * @param targetId 대상 ID
     */
    private void checkAndApplyAutoAction(ReportTargetType targetType, Long targetId) {
        Long reportCount = reportRepository.countByTargetTypeAndTargetId(targetType, targetId);

        if (reportCount >= 10) {
            log.warn("신고 누적 10회 이상 - 자동 조치 실행: {} {}", targetType, targetId);
            executeAction(targetType, targetId);
        }
    }

    /**
     * 제재 조치 실행 (Soft Delete)
     * 
     * @param targetType 대상 타입
     * @param targetId 대상 ID
     */
    private void executeAction(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case FEED -> {
                Feed feed = feedRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다."));
                feed.delete();
                log.info("피드 삭제 조치 완료 - Feed ID: {}", targetId);
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
                comment.delete();
                log.info("댓글 삭제 조치 완료 - Comment ID: {}", targetId);
            }
            case TOGETHER -> {
                Together together = togetherRepository.findById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));
                // TODO: Together 엔티티에 delete() 메서드 추가 필요
                // together.delete();
                log.warn("모임 삭제 조치 - Together ID: {} (delete 메서드 미구현)", targetId);
            }
        }
    }
}
