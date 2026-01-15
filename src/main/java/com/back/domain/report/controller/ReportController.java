package com.back.domain.report.controller;

import com.back.domain.report.dto.request.ReportCreateRequest;
import com.back.domain.report.dto.response.ReportResponse;
import com.back.domain.report.dto.response.ReportSummaryResponse;
import com.back.domain.report.entity.Report;
import com.back.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 신고 컨트롤러 (사용자용)
 * 신고 생성, 내가 한 신고 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    
    /**
     * SecurityContext에서 현재 로그인한 회원 ID 추출
     * @return 현재 로그인한 회원 ID
     */
    private Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        return (Long) authentication.getPrincipal();
    }

    /**
     * 신고 생성
     * 
     * POST /api/reports
     * 
     * @param request 신고 생성 요청
     * @return 생성된 신고 ID
     */
    @PostMapping
    public ResponseEntity<Long> createReport(
            @Valid @RequestBody ReportCreateRequest request
    ) {
        Long currentMemberId = getCurrentMemberId();

        Long reportId = reportService.createReport(
                request.getTargetType(),
                request.getTargetId(),
                request.getReasonType(),
                request.getReasonDetail(),
                currentMemberId
        );

        log.info("신고 생성 API 호출 - 신고자: {}, 대상: {} {}", 
                currentMemberId, request.getTargetType(), request.getTargetId());

        return ResponseEntity.ok(reportId);
    }

    /**
     * 내가 한 신고 목록 조회
     * 
     * GET /api/reports/my?page=0&size=20
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 신고 목록
     */
    @GetMapping("/my")
    public ResponseEntity<Page<ReportSummaryResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentMemberId = getCurrentMemberId();

        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportService.getMyReports(currentMemberId, pageable);

        Page<ReportSummaryResponse> response = reports.map(ReportSummaryResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * 내가 한 신고 상세 조회
     * 
     * GET /api/reports/{reportId}
     * 
     * @param reportId 신고 ID
     * @return 신고 상세 정보
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long reportId
    ) {
        Long currentMemberId = getCurrentMemberId();

        Report report = reportService.getReport(reportId);

        // 본인이 한 신고만 조회 가능
        if (!report.getReporter().getId().equals(currentMemberId)) {
            return ResponseEntity.status(403).build();
        }

        ReportResponse response = ReportResponse.from(report);

        return ResponseEntity.ok(response);
    }
}
