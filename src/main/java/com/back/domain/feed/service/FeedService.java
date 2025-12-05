package com.back.domain.feed.service;

import com.back.domain.feed.dto.feed.request.FeedCreateRequest;
import com.back.domain.feed.dto.feed.request.FeedImageRequest;
import com.back.domain.feed.dto.feed.request.FeedSearchRequest;
import com.back.domain.feed.dto.feed.request.FeedUpdateRequest;
import com.back.domain.feed.dto.feed.response.FeedResponse;
import com.back.domain.feed.dto.feed.response.FeedSummaryResponse;
import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedBookmark;
import com.back.domain.feed.entity.FeedImage;
import com.back.domain.feed.entity.FeedReaction;
import com.back.domain.feed.repository.FeedBookmarkRepository;
import com.back.domain.feed.repository.FeedReactionRepository;
import com.back.domain.feed.repository.FeedRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedReactionRepository feedReactionRepository;
    private final FeedBookmarkRepository feedBookmarkRepository;
    private final TagService tagService;

    /**
     * 피드 생성
     */
    @Transactional
    public Long createFeed(FeedCreateRequest request, Long currentMemberId) {
        // 1. 태그 검증 및 정제
        List<String> validatedTags = tagService.validateAndRefineTags(request.getTags());

        // 2. Feed 엔티티 생성
        Feed feed = Feed.builder()
                .feedType(request.getFeedType())
                .content(request.getContent())
                .visibility(request.getVisibility())
                .tags(validatedTags)
                // .member(memberRepository.findById(currentMemberId).orElseThrow())  // Member 연결 후
                .build();

        // 3. 이미지 추가
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                FeedImageRequest imageReq = request.getImages().get(i);
                FeedImage feedImage = FeedImage.builder()
                        .feed(feed)
                        .imageUrl(imageReq.getImageUrl())
                        .width(imageReq.getWidth())
                        .height(imageReq.getHeight())
                        .displayOrder(imageReq.getDisplayOrder() != null ? imageReq.getDisplayOrder() : i)
                        .fileSize(imageReq.getFileSize())
                        .originalFileName(imageReq.getOriginalFileName())
                        .build();
                feed.addImage(feedImage);
            }
        }

        // 4. 함께하기 연결 (Together 엔티티 연결 후)
        // if (request.getTogetherId() != null) {
        //     Together together = togetherRepository.findById(request.getTogetherId())
        //         .orElseThrow(() -> new IllegalArgumentException("함께하기를 찾을 수 없습니다."));
        //     feed.setTogether(together);
        // }

        Feed savedFeed = feedRepository.save(feed);
        log.info("피드 생성 완료 - ID: {}, 작성자: {}, 태그 수: {}", 
                savedFeed.getId(), currentMemberId, validatedTags.size());

        return savedFeed.getId();
    }

    /**
     * 피드 상세 조회
     */
    public FeedResponse getFeed(Long feedId, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        // 현재 사용자의 리액션/북마크 여부 확인
        boolean isReacted = currentMemberId != null && 
                feedReactionRepository.existsByFeedIdAndMemberId(feedId, currentMemberId);
        boolean isBookmarked = currentMemberId != null && 
                feedBookmarkRepository.existsByFeedIdAndMemberId(feedId, currentMemberId);

        return FeedResponse.from(feed, isReacted, isBookmarked);
    }

    /**
     * 피드 목록 조회 (페이징)
     */
    public Page<FeedSummaryResponse> getFeedList(FeedSearchRequest searchRequest) {
        Pageable pageable = PageRequest.of(
                searchRequest.getPageOrDefault(),
                searchRequest.getSizeOrDefault(),
                getSort(searchRequest.getSortByOrDefault())
        );

        Page<Feed> feedPage;

        // 검색 조건에 따라 다른 쿼리 실행
        if (searchRequest.getFeedType() != null) {
            feedPage = feedRepository.findByFeedTypeAndDeletedAtIsNull(
                    searchRequest.getFeedType(), pageable);
        } else if (searchRequest.getMemberId() != null) {
            feedPage = feedRepository.findByMemberIdAndDeletedAtIsNull(
                    searchRequest.getMemberId(), pageable);
        } else if (searchRequest.getTags() != null && !searchRequest.getTags().isEmpty()) {
            // 태그 검색은 List 반환이므로 별도 처리
            List<Feed> feeds = feedRepository.findByTagsIn(searchRequest.getTags(), pageable);
            // List를 Page로 변환
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), feeds.size());
            List<Feed> pageContent = feeds.subList(start, end);
            feedPage = new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, feeds.size());
        } else {
            feedPage = feedRepository.findByDeletedAtIsNull(pageable);
        }

        return feedPage.map(FeedSummaryResponse::from);
    }

    /**
     * 피드 무한 스크롤 조회
     */
    public List<FeedSummaryResponse> getFeedListInfiniteScroll(Long lastFeedId) {
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;
        
        List<Feed> feeds = feedRepository
                .findTop20ByIdLessThanAndDeletedAtIsNullOrderByIdDesc(cursorId);

        return feeds.stream()
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 피드 수정
     */
    @Transactional
    public void updateFeed(Long feedId, FeedUpdateRequest request, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        // 권한 체크 (작성자만 수정 가능)
        // if (!feed.getMember().getId().equals(currentMemberId)) {
        //     throw new IllegalArgumentException(ErrorCode.FEED_FORBIDDEN.getMessage());
        // }

        // 내용 수정
        if (request.getContent() != null) {
            feed.updateContent(request.getContent());
        }

        // 태그 수정
        if (request.getTags() != null) {
            List<String> validatedTags = tagService.validateAndRefineTags(request.getTags());
            feed.clearTags();
            validatedTags.forEach(feed::addTag);
        }

        // 이미지 수정
        if (request.getImages() != null) {
            feed.clearImages();
            for (int i = 0; i < request.getImages().size(); i++) {
                FeedImageRequest imageReq = request.getImages().get(i);
                FeedImage feedImage = FeedImage.builder()
                        .feed(feed)
                        .imageUrl(imageReq.getImageUrl())
                        .width(imageReq.getWidth())
                        .height(imageReq.getHeight())
                        .displayOrder(imageReq.getDisplayOrder() != null ? imageReq.getDisplayOrder() : i)
                        .fileSize(imageReq.getFileSize())
                        .originalFileName(imageReq.getOriginalFileName())
                        .build();
                feed.addImage(feedImage);
            }
        }

        // 공개 범위 수정
        if (request.getVisibility() != null) {
            feed.updateVisibility(request.getVisibility());
        }

        log.info("피드 수정 완료 - ID: {}", feedId);
    }

    /**
     * 피드 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteFeed(Long feedId, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        // 권한 체크
        // if (!feed.getMember().getId().equals(currentMemberId)) {
        //     throw new IllegalArgumentException(ErrorCode.FEED_FORBIDDEN.getMessage());
        // }

        feed.delete();
        log.info("피드 삭제 완료 - ID: {}", feedId);
    }

    /**
     * 피드 리액션 토글 (좋아요)
     */
    @Transactional
    public boolean toggleReaction(Long feedId, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        // 기존 리액션 확인
        boolean exists = feedReactionRepository.existsByFeedIdAndMemberId(feedId, currentMemberId);

        if (exists) {
            // 리액션 취소
            feedReactionRepository.deleteByFeedIdAndMemberId(feedId, currentMemberId);
            log.info("피드 리액션 취소 - 피드 ID: {}, 회원 ID: {}", feedId, currentMemberId);
            return false;
        } else {
            // 리액션 생성
            FeedReaction reaction = FeedReaction.builder()
                    .feed(feed)
                    // .member(memberRepository.findById(currentMemberId).orElseThrow())
                    .build();
            feedReactionRepository.save(reaction);
            log.info("피드 리액션 생성 - 피드 ID: {}, 회원 ID: {}", feedId, currentMemberId);
            return true;
        }
    }

    /**
     * 피드 북마크 토글
     */
    @Transactional
    public boolean toggleBookmark(Long feedId, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        boolean exists = feedBookmarkRepository.existsByFeedIdAndMemberId(feedId, currentMemberId);

        if (exists) {
            // 북마크 취소
            feedBookmarkRepository.deleteByFeedIdAndMemberId(feedId, currentMemberId);
            log.info("피드 북마크 취소 - 피드 ID: {}, 회원 ID: {}", feedId, currentMemberId);
            return false;
        } else {
            // 북마크 생성
            FeedBookmark bookmark = FeedBookmark.builder()
                    .feed(feed)
                    // .member(memberRepository.findById(currentMemberId).orElseThrow())
                    .build();
            feedBookmarkRepository.save(bookmark);
            log.info("피드 북마크 생성 - 피드 ID: {}, 회원 ID: {}", feedId, currentMemberId);
            return true;
        }
    }

    /**
     * 태그로 피드 검색
     */
    public List<FeedSummaryResponse> searchByTag(String tag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<Feed> feeds = feedRepository.findByTag(tag, pageable);
        
        return feeds.stream()
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 인기 피드 조회 (최근 7일 기준)
     */
    public List<FeedSummaryResponse> getPopularFeeds(int size) {
        Pageable pageable = PageRequest.of(0, size);
        // LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        // List<Feed> feeds = feedRepository.findPopularFeedsSince(weekAgo, pageable);
        
        // 임시로 리액션 많은 순으로
        List<Feed> feeds = feedRepository.findTop20ByDeletedAtIsNullOrderByReactionCountDescCreatedAtDesc();
        
        return feeds.stream()
                .limit(size)
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 정렬 옵션 생성
     */
    private Sort getSort(String sortBy) {
        return switch (sortBy) {
            case "popular" -> Sort.by("reactionCount").descending()
                    .and(Sort.by("createdAt").descending());
            case "comments" -> Sort.by("commentCount").descending()
                    .and(Sort.by("createdAt").descending());
            default -> Sort.by("createdAt").descending();  // latest
        };
    }
}
