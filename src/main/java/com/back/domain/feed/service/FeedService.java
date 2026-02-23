package com.back.domain.feed.service;

import com.back.domain.feed.dto.feed.request.FeedCreateRequest;
import com.back.domain.feed.dto.feed.request.FeedSearchCondition;
import com.back.domain.feed.dto.feed.request.FeedSearchRequest;
import com.back.domain.feed.dto.feed.request.FeedUpdateRequest;
import com.back.domain.feed.dto.feed.response.FeedResponse;
import com.back.domain.feed.dto.feed.response.FeedSummaryResponse;
import com.back.domain.feed.dto.feed.response.InfiniteScrollResponse;
import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedBookmark;
import com.back.domain.feed.entity.FeedImage;
import com.back.domain.feed.entity.FeedReaction;
import com.back.domain.feed.repository.FeedBookmarkRepository;
import com.back.domain.feed.repository.FeedReactionRepository;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.notification.entity.NotificationTargetType;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.service.NotificationService;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedReactionRepository feedReactionRepository;
    private final FeedBookmarkRepository feedBookmarkRepository;
    private final MemberRepository memberRepository;
    private final TagService tagService;
    private final com.back.domain.together.repository.TogetherRepository togetherRepository;
    private final MemberTagStatisticsService memberTagStatisticsService;
    private final NotificationService notificationService;

    /**
     * 피드 생성
     */
    @Transactional
    public Long createFeed(FeedCreateRequest request, Long currentMemberId) {
        // 1. 태그 검증 및 정제
        List<String> validatedTags = tagService.validateAndRefineTags(request.getTags());

        // 2. Together 조회 (togetherId가 있는 경우)
        com.back.domain.together.entity.Together together = null;
        if (request.getTogetherId() != null) {
            together = togetherRepository.findById(request.getTogetherId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 함께하기입니다. ID: " + request.getTogetherId()));
        }

        // 2-1. TOGETHER_VERIFICATION인 경우 하루 1회 체크
        if (request.getFeedType() == com.back.domain.feed.entity.FeedType.TOGETHER_VERIFICATION && together != null) {
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).atStartOfDay();
            Long todayVerificationCount = feedRepository.countTodayVerificationByMemberAndTogether(
                    currentMemberId,
                    request.getTogetherId(),
                    startOfDay
            );
            
            if (todayVerificationCount != null && todayVerificationCount > 0) {
                throw new IllegalArgumentException("이미 오늘 인증을 완료했습니다. 하루에 1번만 인증할 수 있습니다.");
            }
        }

        // 3. Feed 엔티티 생성
        Feed feed = Feed.builder()
                .feedType(request.getFeedType())
                .content(request.getContent())
                .visibility(request.getVisibility())
                .tags(validatedTags)
                .images(new ArrayList<>())
                .bookmarkCount(0)
                .commentCount(0)
                .reactionCount(0)
                .member(memberRepository.findById(currentMemberId).orElseThrow())
                .together(together)
                .build();

        // 4. 이미지 추가
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            request.getImages().forEach(imageReq -> {
                FeedImage feedImage = FeedImage.builder()
                        .feed(feed)
                        .imageUrl(imageReq.getImageUrl())
                        .width(imageReq.getWidth())
                        .height(imageReq.getHeight())
                        .displayOrder(imageReq.getDisplayOrder())
                        .fileSize(imageReq.getFileSize())
                        .originalFileName(imageReq.getOriginalFileName())
                        .build();
                feed.addImage(feedImage);
            });
        }

        Feed savedFeed = feedRepository.save(feed);
        log.info("피드 생성 완료 - ID: {}, Together ID: {}", savedFeed.getId(), 
                 together != null ? together.getId() : "null");

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
     * 피드 목록 조회 (QueryDSL 동적 검색 + 무한 스크롤)
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getFeedList(FeedSearchRequest searchRequest, Long currentMemberId) {
        Long cursorId = searchRequest.getLastFeedId() != null ? searchRequest.getLastFeedId() : Long.MAX_VALUE;
        int requestedSize = searchRequest.getSizeOrDefault();

        FeedSearchCondition condition = FeedSearchCondition.from(searchRequest);

        List<Feed> feeds = feedRepository.searchFeedsForInfiniteScroll(condition, cursorId, requestedSize + 1);

        return createInfiniteScrollResponse(feeds, requestedSize, currentMemberId);
    }

    /**
     * 전체 피드 무한 스크롤 (커서 기반)
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getFeedListInfiniteScroll(
            Long lastFeedId,
            Integer size
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;
        Long currentMemberId = getCurrentMemberIdOrNull();

        List<Feed> feeds;

        if (lastFeedId == null && currentMemberId != null) {
            feeds = getMixedRecommendedAndRegularFeeds(currentMemberId, requestedSize);
        } else {
            feeds = feedRepository.findFeedsForInfiniteScroll(cursorId, requestedSize + 1);
        }

        return createInfiniteScrollResponse(feeds, requestedSize, currentMemberId);
    }

    /**
     * getCurrentMemberIdOrNull() 헬퍼 메서드 (로그인 안 한 경우 null 반환)
     */
    private Long getCurrentMemberIdOrNull() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            return (Long) authentication.getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 특정 회원의 피드 무한 스크롤 (커서 기반)
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getMemberFeedsInfiniteScroll(
            Long memberId,
            Long lastFeedId,
            Integer size,
            Long currentMemberId
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        List<Feed> feeds = feedRepository.findMemberFeedsForInfiniteScroll(memberId, cursorId, requestedSize + 1);

        return createInfiniteScrollResponse(feeds, requestedSize, currentMemberId);
    }

    /**
     * 특정 Together의 피드 무한 스크롤 (커서 기반)
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getTogetherFeedsInfiniteScroll(
            Long togetherId,
            Long lastFeedId,
            Integer size,
            Long currentMemberId
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        boolean isFirstPage = (lastFeedId == null);
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        List<Feed> feeds = feedRepository.findTogetherFeedsForInfiniteScroll(
                togetherId, cursorId, requestedSize + 1, isFirstPage
        );

        if (isFirstPage) {
            List<Feed> unpinnedFeeds = feeds.stream().filter(f -> !f.getIsPinned()).toList();
            List<Feed> pinnedFeeds = feeds.stream().filter(Feed::getIsPinned).toList();

            boolean hasNext = unpinnedFeeds.size() > (requestedSize - pinnedFeeds.size());
            int unpinnedSlotSize = requestedSize - pinnedFeeds.size();
            List<Feed> actualUnpinned = hasNext ? unpinnedFeeds.subList(0, unpinnedSlotSize) : unpinnedFeeds;

            List<Feed> actualFeeds = new ArrayList<>(pinnedFeeds);
            actualFeeds.addAll(actualUnpinned);

            Set<Long> reactedFeedIds = extractReactedFeedIds(actualFeeds, currentMemberId);
            Set<Long> bookmarkedFeedIds = extractBookmarkedFeedIds(actualFeeds, currentMemberId);

            List<FeedSummaryResponse> responses = actualFeeds.stream()
                    .map(feed -> FeedSummaryResponse.from(feed, reactedFeedIds, bookmarkedFeedIds))
                    .collect(Collectors.toList());

            Long nextCursor = actualUnpinned.isEmpty() ? null :
                    actualUnpinned.get(actualUnpinned.size() - 1).getId();

            return InfiniteScrollResponse.<FeedSummaryResponse>builder()
                    .content(responses)
                    .nextCursor(nextCursor)
                    .hasNext(hasNext)
                    .size(responses.size())
                    .requestedSize(requestedSize)
                    .build();
        }

        return createInfiniteScrollResponse(feeds, requestedSize, currentMemberId);
    }

    /**
     * 피드 수정
     */
    @Transactional
    public FeedResponse updateFeed(Long feedId, FeedUpdateRequest request, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        // 권한 체크 (작성자만 수정 가능)
        if (!feed.getMember().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException(ErrorCode.FEED_FORBIDDEN.getMessage());
        }

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
            request.getImages().forEach(imageReq -> {
                FeedImage feedImage = FeedImage.builder()
                        .feed(feed)
                        .imageUrl(imageReq.getImageUrl())
                        .width(imageReq.getWidth())
                        .height(imageReq.getHeight())
                        .displayOrder(imageReq.getDisplayOrder())
                        .fileSize(imageReq.getFileSize())
                        .originalFileName(imageReq.getOriginalFileName())
                        .build();
                feed.addImage(feedImage);
            });
        }

        // 공개 범위 수정
        if (request.getVisibility() != null) {
            feed.updateVisibility(request.getVisibility());
        }

        log.info("피드 수정 완료 - ID: {}", feedId);

        // 수정된 피드의 리액션/북마크 여부 확인 후 FeedResponse 반환
        boolean isReacted = feedReactionRepository.existsByFeedIdAndMemberId(feedId, currentMemberId);
        boolean isBookmarked = feedBookmarkRepository.existsByFeedIdAndMemberId(feedId, currentMemberId);

        return FeedResponse.from(feed, isReacted, isBookmarked);
    }

    /**
     * 피드 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteFeed(Long feedId, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        // 권한 체크
        if (!feed.getMember().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException(ErrorCode.FEED_FORBIDDEN.getMessage());
        }

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

        boolean exists = feedReactionRepository.existsByFeedIdAndMemberId(feedId, currentMemberId);

        if (exists) {
            // 리액션 취소
            feedReactionRepository.deleteByFeedIdAndMemberId(feedId, currentMemberId);
            log.info("피드 리액션 취소 - 피드 ID: {}, 회원 ID: {}", feedId, currentMemberId);
            
            // 통계 비동기 업데이트
            memberTagStatisticsService.updateStatisticsAsync(currentMemberId);
            
            return false;
        } else {
            // 리액션 생성
            FeedReaction reaction = FeedReaction.builder()
                    .feed(feed)
                    .member(memberRepository.findById(currentMemberId).orElseThrow())
                    .build();
            feedReactionRepository.save(reaction);
            log.info("피드 리액션 생성 - 피드 ID: {}, 회원 ID: {}", feedId, currentMemberId);

            // 알림 발송 (피드 작성자에게)
            notificationService.createNotification(
                    feed.getMember().getId(),
                    currentMemberId,
                    NotificationType.FEED_REACTION,
                    NotificationTargetType.FEED,
                    feedId
            );

            // 통계 비동기 업데이트
            memberTagStatisticsService.updateStatisticsAsync(currentMemberId);

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
                    .member(memberRepository.findById(currentMemberId).orElseThrow())
                    .build();
            feedBookmarkRepository.save(bookmark);
            log.info("피드 북마크 생성 - 피드 ID: {}, 회원 ID: {}", feedId, currentMemberId);
            return true;
        }
    }

    /**
     * 태그 검색 무한 스크롤
     */
    public InfiniteScrollResponse<FeedSummaryResponse> searchByTagInfiniteScroll(
            String tag,
            Long lastFeedId,
            Integer size,
            Long currentMemberId
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        List<Feed> feeds = feedRepository.findByTagForInfiniteScroll(tag, cursorId, requestedSize + 1);

        return createInfiniteScrollResponse(feeds, requestedSize, currentMemberId);
    }

    /**
     * 북마크 피드 무한 스크롤
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getBookmarkedFeedsInfiniteScroll(
            Long memberId,
            Long lastFeedId,
            Integer size,
            Long currentMemberId
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        Pageable pageable = PageRequest.of(0, requestedSize + 1);
        Page<Feed> feedPage = feedBookmarkRepository.findBookmarkedFeedsByMemberIdForInfiniteScroll(
            memberId, cursorId, pageable
        );

        List<Feed> feeds = feedPage.getContent();

        return createInfiniteScrollResponse(feeds, requestedSize, currentMemberId);
    }

    /**
     * 인기 피드 조회 (최근 7일 기준, QueryDSL)
     */
    public List<FeedSummaryResponse> getPopularFeeds(int size, Long currentMemberId) {
        int validatedSize = Math.min(Math.max(size, 1), 50);

        FeedSearchCondition condition = FeedSearchCondition.builder()
                .startDate(LocalDateTime.now().minusDays(7))
                .build();

        List<Feed> feeds = feedRepository.findPopularFeedsWithCondition(condition, validatedSize);

        Set<Long> reactedFeedIds = extractReactedFeedIds(feeds, currentMemberId);
        Set<Long> bookmarkedFeedIds = extractBookmarkedFeedIds(feeds, currentMemberId);

        return feeds.stream()
                .map(feed -> FeedSummaryResponse.from(feed, reactedFeedIds, bookmarkedFeedIds))
                .collect(Collectors.toList());
    }

    /**
     * 댓글 많은 피드 Top N
     */
    public List<FeedSummaryResponse> getMostCommentedFeeds(int size, Long currentMemberId) {
        int validatedSize = Math.min(Math.max(size, 1), 50);

        List<Feed> feeds = feedRepository.findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();

        Set<Long> reactedFeedIds = extractReactedFeedIds(feeds, currentMemberId);
        Set<Long> bookmarkedFeedIds = extractBookmarkedFeedIds(feeds, currentMemberId);

        return feeds.stream()
                .limit(validatedSize)
                .map(feed -> FeedSummaryResponse.from(feed, reactedFeedIds, bookmarkedFeedIds))
                .collect(Collectors.toList());
    }

    /**
     * 북마크 많은 피드 Top N
     */
    public List<FeedSummaryResponse> getMostBookmarkedFeeds(int size, Long currentMemberId) {
        int validatedSize = Math.min(Math.max(size, 1), 50);

        List<Feed> feeds = feedRepository.findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();

        Set<Long> reactedFeedIds = extractReactedFeedIds(feeds, currentMemberId);
        Set<Long> bookmarkedFeedIds = extractBookmarkedFeedIds(feeds, currentMemberId);

        return feeds.stream()
                .limit(validatedSize)
                .map(feed -> FeedSummaryResponse.from(feed, reactedFeedIds, bookmarkedFeedIds))
                .collect(Collectors.toList());
    }

    /**
     * 특정 회원의 북마크 개수
     */
    public Long getBookmarkCount(Long memberId) {
        return feedBookmarkRepository.countByMemberId(memberId);
    }

    // ========== 공지 피드 관련 ==========

    /**
     * 특정 Together의 공지 피드 목록 조회
     */
    public List<FeedSummaryResponse> getTogetherNoticeFeeds(Long togetherId, Long currentMemberId) {
        List<Feed> feeds = feedRepository.findByTogether_IdAndFeedTypeAndDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc(
                togetherId,
                com.back.domain.feed.entity.FeedType.TOGETHER_NOTICE
        );

        Set<Long> reactedFeedIds = extractReactedFeedIds(feeds, currentMemberId);
        Set<Long> bookmarkedFeedIds = extractBookmarkedFeedIds(feeds, currentMemberId);

        return feeds.stream()
                .map(feed -> FeedSummaryResponse.from(feed, reactedFeedIds, bookmarkedFeedIds))
                .collect(Collectors.toList());
    }

    /**
     * 특정 Together의 상단 고정된 공지 피드만 조회
     */
    public List<FeedSummaryResponse> getPinnedNoticeFeeds(Long togetherId, Long currentMemberId) {
        List<Feed> feeds = feedRepository.findByTogether_IdAndFeedTypeAndIsPinnedTrueAndDeletedAtIsNullOrderByCreatedAtDesc(
                togetherId,
                com.back.domain.feed.entity.FeedType.TOGETHER_NOTICE
        );

        Set<Long> reactedFeedIds = extractReactedFeedIds(feeds, currentMemberId);
        Set<Long> bookmarkedFeedIds = extractBookmarkedFeedIds(feeds, currentMemberId);

        return feeds.stream()
                .map(feed -> FeedSummaryResponse.from(feed, reactedFeedIds, bookmarkedFeedIds))
                .collect(Collectors.toList());
    }

    /**
     * 공지 피드 상단 고정 토글
     * 
     * @param feedId 피드 ID
     * @param currentMemberId 현재 사용자 ID
     * @return 고정 여부 (true: 고정됨, false: 고정 해제됨)
     */
    @Transactional
    public boolean togglePinNotice(Long feedId, Long currentMemberId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));

        // 공지 피드만 고정 가능
        if (!feed.isTogetherNoticeFeed()) {
            throw new IllegalArgumentException("공지 피드만 상단에 고정할 수 있습니다.");
        }

        // Together 모임장 권한 체크
        if (feed.getTogether() == null) {
            throw new IllegalArgumentException("함께하기 정보를 찾을 수 없습니다.");
        }
        
        if (!feed.getTogether().getMember().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException("방장만 공지를 고정할 수 있습니다.");
        }

        // 고정 토글
        if (feed.getIsPinned()) {
            feed.unpin();
            log.info("공지 피드 고정 해제 - Feed ID: {}", feedId);
            return false;
        } else {
            // 다음 pinOrder 계산 (기존 최대값 + 1)
            Integer maxPinOrder = feedRepository.findMaxPinOrderByTogetherId(feed.getTogether().getId());
            Integer nextPinOrder = maxPinOrder + 1;
            
            feed.pin(nextPinOrder);
            log.info("공지 피드 고정 - Feed ID: {}, pinOrder: {}", feedId, nextPinOrder);
            return true;
        }
    }

    // ========== Private 헬퍼 메서드 ==========

    /**
     * 추천 피드와 일반 피드를 섞어서 반환 (첫 페이지용)
     * 
     * @param memberId 회원 ID
     * @param totalSize 총 반환할 개수
     * @return 섞인 피드 리스트
     */
    private List<Feed> getMixedRecommendedAndRegularFeeds(Long memberId, int totalSize) {
        // 1. 태그 기반 추천 피드 조회 (3개)
        List<Feed> recommendedFeeds = getTagBasedRecommendations(memberId, 3);
        
        // 2. 추천된 피드 ID 수집 (중복 방지용)
        List<Long> excludeFeedIds = recommendedFeeds.stream()
                .map(Feed::getId)
                .collect(Collectors.toList());
        
        // 3. 일반 피드 조회 (totalSize - 추천개수 + 1)
        int regularFeedCount = totalSize - recommendedFeeds.size() + 1; // hasNext 체크용 +1
        List<Feed> regularFeeds = feedRepository.findFeedsForInfiniteScrollExcluding(
            Long.MAX_VALUE, 
            excludeFeedIds, 
            regularFeedCount
        );
        
        // 4. 섞기 (추천 피드를 상위에 배치: 위치 0, 2, 5)
        return mixFeedsWithTopFocus(recommendedFeeds, regularFeeds, totalSize);
    }

    /**
     * 태그 기반 추천 피드 조회
     * 
     * @param memberId 회원 ID
     * @param count 추천 개수
     * @return 추천 피드 리스트
     */
    private List<Feed> getTagBasedRecommendations(Long memberId, int count) {
        // 1. 사용자가 좋아요 누른 피드의 자주 사용된 태그 조회 (캐싱된 통계에서)
        List<String> frequentTags = memberTagStatisticsService.getFrequentTags(memberId);
        
        if (frequentTags.isEmpty()) {
            // 태그가 없으면 빈 리스트 반환
            return List.of();
        }
        
        // 최대 5개만 사용
        List<String> topTags = frequentTags.stream()
                .limit(5)
                .collect(Collectors.toList());
        
        // 2. 이미 좋아요 누른 피드 ID 조회 (제외용)
        List<Long> reactedFeedIds = feedReactionRepository.findReactedFeedIdsByMemberId(memberId);
        
        // 3. 추천 피드 조회 (태그 매칭 + 인기도 + 최신성)
        return feedRepository.findRecommendedFeedsByTags(topTags, memberId, reactedFeedIds, count);
    }

    /**
     * 추천 피드와 일반 피드 섞기 (상위 집중 배치)
     * 
     * @param recommended 추천 피드 리스트
     * @param regular 일반 피드 리스트
     * @param totalSize 총 반환할 개수
     * @return 섞인 피드 리스트
     */
    private List<Feed> mixFeedsWithTopFocus(List<Feed> recommended, List<Feed> regular, int totalSize) {
        List<Feed> result = new ArrayList<>();
        
        // 추천 피드 배치 위치 (0, 2, 5)
        int[] recommendedPositions = {0, 2, 5};
        int recIndex = 0;
        int regIndex = 0;
        
        for (int i = 0; i < totalSize + 1; i++) { // +1은 hasNext 체크용
            // 현재 위치가 추천 피드 배치 위치인지 확인
            boolean isRecommendedPosition = false;
            for (int pos : recommendedPositions) {
                if (i == pos) {
                    isRecommendedPosition = true;
                    break;
                }
            }
            
            if (isRecommendedPosition && recIndex < recommended.size()) {
                // 추천 피드 배치
                result.add(recommended.get(recIndex++));
            } else if (regIndex < regular.size()) {
                // 일반 피드 배치
                result.add(regular.get(regIndex++));
            } else {
                // 더 이상 피드가 없으면 종료
                break;
            }
        }
        
        return result;
    }

    /**
     * 무한 스크롤 응답 생성 (공통 로직)
     */
    private InfiniteScrollResponse<FeedSummaryResponse> createInfiniteScrollResponse(
            List<Feed> feeds,
            int requestedSize,
            Long currentMemberId
    ) {
        boolean hasNext = feeds.size() > requestedSize;
        List<Feed> actualFeeds = hasNext ? feeds.subList(0, requestedSize) : feeds;

        Set<Long> reactedFeedIds = extractReactedFeedIds(actualFeeds, currentMemberId);
        Set<Long> bookmarkedFeedIds = extractBookmarkedFeedIds(actualFeeds, currentMemberId);

        List<FeedSummaryResponse> responses = actualFeeds.stream()
                .map(feed -> FeedSummaryResponse.from(feed, reactedFeedIds, bookmarkedFeedIds))
                .collect(Collectors.toList());

        Long nextCursor = actualFeeds.isEmpty() ? null :
                actualFeeds.get(actualFeeds.size() - 1).getId();

        return InfiniteScrollResponse.<FeedSummaryResponse>builder()
                .content(responses)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(responses.size())
                .requestedSize(requestedSize)
                .build();
    }

    /**
     * 배치 조회로 현재 사용자가 좋아요한 피드 ID Set 반환 (N+1 방지)
     */
    private Set<Long> extractReactedFeedIds(List<Feed> feeds, Long currentMemberId) {
        if (currentMemberId == null || feeds.isEmpty()) {
            return Set.of();
        }
        List<Long> feedIds = feeds.stream().map(Feed::getId).collect(Collectors.toList());
        return new HashSet<>(feedReactionRepository.findReactedFeedIdsByMemberIdAndFeedIdIn(currentMemberId, feedIds));
    }

    /**
     * 배치 조회로 현재 사용자가 북마크한 피드 ID Set 반환 (N+1 방지)
     */
    private Set<Long> extractBookmarkedFeedIds(List<Feed> feeds, Long currentMemberId) {
        if (currentMemberId == null || feeds.isEmpty()) {
            return Set.of();
        }
        List<Long> feedIds = feeds.stream().map(Feed::getId).collect(Collectors.toList());
        return new HashSet<>(feedBookmarkRepository.findBookmarkedFeedIdsByMemberIdAndFeedIdIn(currentMemberId, feedIds));
    }

    // ========== 회원 통계 ==========

    /**
     * 특정 회원이 작성한 피드 개수
     * Member 도메인의 GET /api/members/me 응답에 사용
     * 
     * @param memberId 회원 ID
     * @return 작성한 피드 개수
     */
    public Long getMemberFeedCount(Long memberId) {
        return feedRepository.countByMemberIdAndDeletedAtIsNull(memberId);
    }

    /**
     * 특정 회원이 누른 좋아요(리액션) 개수
     * Member 도메인의 GET /api/members/me 응답에 사용
     * 
     * @param memberId 회원 ID
     * @return 누른 좋아요 개수
     */
    public Long getMemberReactionCount(Long memberId) {
        return feedReactionRepository.countByMemberId(memberId);
    }
}
