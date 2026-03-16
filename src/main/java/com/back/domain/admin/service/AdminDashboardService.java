package com.back.domain.admin.service;

import com.back.domain.admin.dto.response.DashboardStatsResponse;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.donation.repository.DonationRepository;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.report.entity.ReportTargetType;
import com.back.domain.report.service.ReportService;
import com.back.domain.together.repository.TogetherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드 서비스.
 * 기존 도메인 Repository/Service만 주입해 집계 조회 (다른 도메인 코드 수정 없음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final MemberRepository memberRepository;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final TogetherRepository togetherRepository;
    private final DonationRepository donationRepository;
    private final ReportService reportService;

    /**
     * 대시보드용 통계 한 번에 조회
     */
    public DashboardStatsResponse getStats() {
        long memberCount = memberRepository.countByDeletedAtIsNull();
        long feedCount = feedRepository.countByDeletedAtIsNull();
        long commentCount = commentRepository.count();
        long togetherCount = togetherRepository.count();
        long donationCount = donationRepository.count();

        Long reportPendingTotal = reportService.getPendingReportCount();
        Long reportPendingFeed = reportService.getPendingReportCountByType(ReportTargetType.FEED);
        Long reportPendingComment = reportService.getPendingReportCountByType(ReportTargetType.COMMENT);
        Long reportPendingTogether = reportService.getPendingReportCountByType(ReportTargetType.TOGETHER);

        return DashboardStatsResponse.builder()
                .memberCount(memberCount)
                .feedCount(feedCount)
                .commentCount(commentCount)
                .togetherCount(togetherCount)
                .donationCount(donationCount)
                .reportPendingTotal(reportPendingTotal)
                .reportPendingFeed(reportPendingFeed)
                .reportPendingComment(reportPendingComment)
                .reportPendingTogether(reportPendingTogether)
                .build();
    }
}
