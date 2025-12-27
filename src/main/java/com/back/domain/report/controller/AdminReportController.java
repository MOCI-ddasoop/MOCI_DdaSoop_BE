package com.back.domain.report.controller;

import com.back.domain.report.dto.request.ReportProcessRequest;
import com.back.domain.report.dto.response.ReportResponse;
import com.back.domain.report.dto.response.ReportStatsResponse;
import com.back.domain.report.dto.response.ReportSummaryResponse;
import com.back.domain.report.entity.Report;
import com.back.domain.report.entity.ReportStatus;
import com.back.domain.report.entity.ReportTargetType;
import com.back.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 신고 컨트롤러
 * 신고 목록 조회, 신고 처리, 통계 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    // ========== 신고 목록 조회 ==========

    /**
     * 신고 목록 조회 (상태별 필터링)
     * 
     * GET /api/admin/reports?status=PENDING&page=0&size=20
     * 
     * @param status 처리 상태 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 신고 목록
     */
    @GetMapping
    public ResponseEntity<Page<ReportSummaryResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Report> reports;
        if (status != null) {
            reports = reportService.getReportsByStatus(status, pageable);
        } else {
            // 상태 미지정 시 전체 조회 (Repository에 추가 필요)
            reports = reportService.getReportsByStatus(ReportStatus.PENDING, pageable);
        }

        Page<ReportSummaryResponse> response = reports.map(ReportSummaryResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * 타입 + 상태별 신고 목록 조회
     * 
     * GET /api/admin/reports/type/{targetType}?status=PENDING&page=0&size=20
     * 
     * @param targetType 대상 타입 (FEED, COMMENT, TOGETHER)
     * @param status 처리 상태
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 신고 목록
     */
    @GetMapping("/type/{targetType}")
    public ResponseEntity<Page<ReportSummaryResponse>> getReportsByType(
            @PathVariable ReportTargetType targetType,
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Report> reports = reportService.getReportsByTypeAndStatus(
                targetType, status, pageable
        );

        Page<ReportSummaryResponse> response = reports.map(ReportSummaryResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 대상의 신고 목록 조회
     * 
     * GET /api/admin/reports/target?type=FEED&id=123
     * 
     * @param type 대상 타입
     * @param id 대상 ID
     * @return 신고 목록
     */
    @GetMapping("/target")
    public ResponseEntity<List<ReportSummaryResponse>> getReportsByTarget(
            @RequestParam ReportTargetType type,
            @RequestParam Long id
    ) {
        List<Report> reports = reportService.getReportsByTarget(type, id);

        List<ReportSummaryResponse> response = reports.stream()
                .map(ReportSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 회원이 받은 신고 목록 조회
     * 
     * GET /api/admin/reports/member/{memberId}?page=0&size=20
     * 
     * @param memberId 회원 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 신고 목록
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Page<ReportSummaryResponse>> getReportsByMember(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Report> reports = reportService.getReportsReceivedByMember(memberId, pageable);

        Page<ReportSummaryResponse> response = reports.map(ReportSummaryResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * 신고 상세 조회
     * 
     * GET /api/admin/reports/{reportId}
     * 
     * @param reportId 신고 ID
     * @return 신고 상세 정보
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long reportId) {
        Report report = reportService.getReport(reportId);
        ReportResponse response = ReportResponse.from(report);

        return ResponseEntity.ok(response);
    }

    // ========== 신고 처리 ==========

    /**
     * 신고 처리 (승인/기각)
     * 
     * PUT /api/admin/reports/{reportId}/process
     * 
     * @param reportId 신고 ID
     * @param request 처리 요청 (status, adminComment)
     * @return 처리된 신고 정보
     */
    @PutMapping("/{reportId}/process")
    public ResponseEntity<ReportResponse> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequest request
            // TODO: @AuthenticationPrincipal 또는 @CurrentAdmin으로 관리자 ID 가져오기
    ) {
        // 임시: 관리자 ID (실제로는 JWT 토큰에서 추출)
        Long adminId = 4L;  // DevInitData의 admin ID

        if (request.getStatus() == ReportStatus.APPROVED) {
            reportService.approveReport(reportId, adminId, request.getAdminComment());
        } else if (request.getStatus() == ReportStatus.REJECTED) {
            reportService.rejectReport(reportId, adminId, request.getAdminComment());
        } else {
            return ResponseEntity.badRequest().build();
        }

        Report report = reportService.getReport(reportId);
        ReportResponse response = ReportResponse.from(report);

        log.info("신고 처리 완료 - 신고 ID: {}, 상태: {}", reportId, request.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * 검토 시작
     * 
     * PUT /api/admin/reports/{reportId}/review
     * 
     * @param reportId 신고 ID
     * @return 업데이트된 신고 정보
     */
    @PutMapping("/{reportId}/review")
    public ResponseEntity<ReportResponse> startReview(
            @PathVariable Long reportId
            // TODO: @AuthenticationPrincipal 또는 @CurrentAdmin으로 관리자 ID 가져오기
    ) {
        // 임시: 관리자 ID
        Long adminId = 4L;

        reportService.startReview(reportId, adminId);

        Report report = reportService.getReport(reportId);
        ReportResponse response = ReportResponse.from(report);

        return ResponseEntity.ok(response);
    }

    // ========== 통계 조회 ==========

    /**
     * 신고 통계 조회 (대시보드용)
     * 
     * GET /api/admin/reports/stats
     * 
     * @return 신고 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<ReportStatsResponse> getReportStats() {
        Long totalPending = reportService.getPendingReportCount();
        Long feedPending = reportService.getPendingReportCountByType(ReportTargetType.FEED);
        Long commentPending = reportService.getPendingReportCountByType(ReportTargetType.COMMENT);
        Long togetherPending = reportService.getPendingReportCountByType(ReportTargetType.TOGETHER);

        ReportStatsResponse response = ReportStatsResponse.builder()
                .totalPending(totalPending)
                .feedPending(feedPending)
                .commentPending(commentPending)
                .togetherPending(togetherPending)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 신고 많은 콘텐츠 조회
     * 
     * GET /api/admin/reports/frequent-targets?type=FEED&minCount=10&size=20
     * 
     * @param type 대상 타입
     * @param minCount 최소 신고 횟수 (기본: 10)
     * @param size 결과 개수 (기본: 20)
     * @return 대상 ID 목록
     */
    @GetMapping("/frequent-targets")
    public ResponseEntity<List<Long>> getFrequentlyReportedTargets(
            @RequestParam ReportTargetType type,
            @RequestParam(defaultValue = "10") Long minCount,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(0, size);
        List<Long> targetIds = reportService.getFrequentlyReportedTargets(type, minCount, pageable);

        return ResponseEntity.ok(targetIds);
    }

    /**
     * 신고 많이 받은 회원 조회
     * 
     * GET /api/admin/reports/frequent-members?minCount=5&size=20
     * 
     * @param minCount 최소 신고 횟수 (기본: 5)
     * @param size 결과 개수 (기본: 20)
     * @return 회원 ID 목록
     */
    @GetMapping("/frequent-members")
    public ResponseEntity<List<Long>> getFrequentlyReportedMembers(
            @RequestParam(defaultValue = "5") Long minCount,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(0, size);
        List<Long> memberIds = reportService.getFrequentlyReportedMembers(minCount, pageable);

        return ResponseEntity.ok(memberIds);
    }
}
