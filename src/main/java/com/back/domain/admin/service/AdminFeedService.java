package com.back.domain.admin.service;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedVisibility;
import com.back.domain.feed.repository.FeedRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 피드 제어 서비스.
 * FeedRepository만 사용 (feed 도메인 코드 수정 없음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFeedService {

    private final FeedRepository feedRepository;

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
}
