package com.back.domain.admin.service;

import com.back.domain.admin.dto.response.AdminFeedSummaryResponse;
import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedVisibility;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.report.entity.ReportTargetType;
import com.back.domain.report.service.ReportService;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 피드 제어/조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFeedService {

    private final FeedRepository feedRepository;
    private final ReportService reportService;

    /**
     * 피드 강제 삭제 (soft delete)
     */
    @Transactional
    public void forceDeleteFeed(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));
        if (feed.isDeleted()) {
            throw new IllegalArgumentException(ErrorCode.FEED_ALREADY_DELETED.getMessage());
        }
        feed.delete();
        feedRepository.save(feed);
        log.info("관리자 피드 강제 삭제 - feedId: {}", feedId);
    }

    /**
     * 피드 비공개 처리
     */
    @Transactional
    public void setFeedVisibilityPrivate(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));
        if (feed.isDeleted()) {
            throw new IllegalArgumentException(ErrorCode.FEED_ALREADY_DELETED.getMessage());
        }
        feed.updateVisibility(FeedVisibility.PRIVATE);
        feedRepository.save(feed);
        log.info("관리자 피드 비공개 처리 - feedId: {}", feedId);
    }

    /**
     * 관리자용 피드 리스트 조회 (필터 + 페이징).
     * - visibility: 공개 범위 필터
     * - authorId: 작성자 필터
     * - reportedOnly: 신고가 1건 이상 있는 피드만
     */
    @Transactional(readOnly = true)
    public Page<AdminFeedSummaryResponse> getFeedPageForAdmin(
            FeedVisibility visibility,
            Long authorId,
            Boolean reportedOnly,
            Pageable pageable
    ) {
        boolean reportFilter = Boolean.TRUE.equals(reportedOnly);
        Page<Feed> feeds = feedRepository.findAdminFeeds(visibility, authorId, reportFilter, pageable);

        return feeds.map(feed -> {
            Long reportCount = reportService.getReportCount(ReportTargetType.FEED, feed.getId());
            return AdminFeedSummaryResponse.from(feed, reportCount);
        });
    }
}

