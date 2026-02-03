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
    private final MemberRepository memberRepository;
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
                .images(new ArrayList<>())
                .bookmarkCount(0)
                .commentCount(0)
                .reactionCount(0)
                .member(memberRepository.findById(currentMemberId).orElseThrow())  // Member 연결 후
                .build();

        // 3. 이미지 추가
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
        log.info("피드 생성 완료 - ID: {}", savedFeed.getId());

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
     * 피드 목록 조회 (QueryDSL 동적 검색 + 페이징)
     */
    public Page<FeedSummaryResponse> getFeedList(FeedSearchRequest searchRequest) {
        // 1. 페이징 설정
        Pageable pageable = PageRequest.of(
                searchRequest.getPageOrDefault(),
                searchRequest.getSizeOrDefault()
        );

        // 2. FeedSearchRequest → FeedSearchCondition 변환
        FeedSearchCondition condition = FeedSearchCondition.from(searchRequest);

        // 3. QueryDSL로 검색 (동적 쿼리 + Fetch Join)
        Page<Feed> feedPage = feedRepository.searchFeeds(condition, pageable);

        // 4. DTO 변환
        return feedPage.map(FeedSummaryResponse::from);
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

        // 동적 limit 지원 (requestedSize + 1)
        List<Feed> feeds = feedRepository.findFeedsForInfiniteScroll(cursorId, requestedSize + 1);

        return createInfiniteScrollResponse(feeds, requestedSize);
    }

    /**
     * 특정 회원의 피드 무한 스크롤 (커서 기반)
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getMemberFeedsInfiniteScroll(
            Long memberId,
            Long lastFeedId,
            Integer size
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        // 동적 limit 지원 (requestedSize + 1)
        List<Feed> feeds = feedRepository.findMemberFeedsForInfiniteScroll(memberId, cursorId, requestedSize + 1);

        return createInfiniteScrollResponse(feeds, requestedSize);
    }

    /**
     * 특정 Together의 피드 무한 스크롤 (커서 기반)
     * 첫 페이지일 때만 핀 고정 피드 포함, 이후 페이지에서는 일반 피드만 반환
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getTogetherFeedsInfiniteScroll(
            Long togetherId,
            Long lastFeedId,
            Integer size
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        boolean isFirstPage = (lastFeedId == null);
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        List<Feed> feeds = feedRepository.findTogetherFeedsForInfiniteScroll(
                togetherId, cursorId, requestedSize + 1, isFirstPage
        );

        // 첫 페이지일 때 핀 고정 피드와 일반 피드를 분리하여 nextCursor 계산
        if (isFirstPage) {
            List<Feed> unpinnedFeeds = feeds.stream().filter(f -> !f.getIsPinned()).toList();
            List<Feed> pinnedFeeds = feeds.stream().filter(Feed::getIsPinned).toList();

            boolean hasNext = unpinnedFeeds.size() > (requestedSize - pinnedFeeds.size());
            int unpinnedSlotSize = requestedSize - pinnedFeeds.size();
            List<Feed> actualUnpinned = hasNext ? unpinnedFeeds.subList(0, unpinnedSlotSize) : unpinnedFeeds;

            List<Feed> actualFeeds = new ArrayList<>(pinnedFeeds);
            actualFeeds.addAll(actualUnpinned);

            List<FeedSummaryResponse> responses = actualFeeds.stream()
                    .map(FeedSummaryResponse::from)
                    .collect(Collectors.toList());

            // nextCursor는 일반 피드 마지막 아이템의 ID
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

        // 두 번째 페이지 이후: 일반 피드만 반환되므로 공통 로직 사용
        return createInfiniteScrollResponse(feeds, requestedSize);
    }

    /**
     * 피드 수정
     */
    @Transactional
    public void updateFeed(Long feedId, FeedUpdateRequest request, Long currentMemberId) {
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
            return false;
        } else {
            // 리액션 생성
            FeedReaction reaction = FeedReaction.builder()
                    .feed(feed)
                    .member(memberRepository.findById(currentMemberId).orElseThrow())
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
            Integer size
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        List<Feed> feeds = feedRepository.findByTagForInfiniteScroll(tag, cursorId, requestedSize + 1);

        return createInfiniteScrollResponse(feeds, requestedSize);
    }

    /**
     * 북마크 피드 무한 스크롤
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getBookmarkedFeedsInfiniteScroll(
            Long memberId,
            Long lastFeedId,
            Integer size
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        Pageable pageable = PageRequest.of(0, requestedSize + 1);
        Page<Feed> feedPage = feedBookmarkRepository.findBookmarkedFeedsByMemberIdForInfiniteScroll(
            memberId,
            cursorId,
            pageable
        );

        List<Feed> feeds = feedPage.getContent();

        return createInfiniteScrollResponse(feeds, requestedSize);
    }

    /**
     * 인기 피드 조회 (최근 7일 기준, QueryDSL)
     */
    public List<FeedSummaryResponse> getPopularFeeds(int size) {
        // size 검증 추가 (최대 50개)
        int validatedSize = Math.min(Math.max(size, 1), 50);
        
        FeedSearchCondition condition = FeedSearchCondition.builder()
                .startDate(LocalDateTime.now().minusDays(7))  // 최근 7일
                .build();

        List<Feed> feeds = feedRepository.findPopularFeedsWithCondition(condition, validatedSize);

        return feeds.stream()
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 많은 피드 Top N
     */
    public List<FeedSummaryResponse> getMostCommentedFeeds(int size) {
        // size 검증 추가 (최대 50개)
        int validatedSize = Math.min(Math.max(size, 1), 50);
        
        List<Feed> feeds = feedRepository.findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();

        return feeds.stream()
                .limit(validatedSize)
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 북마크 많은 피드 Top N
     */
    public List<FeedSummaryResponse> getMostBookmarkedFeeds(int size) {
        // size 검증 추가 (최대 50개)
        int validatedSize = Math.min(Math.max(size, 1), 50);
        
        List<Feed> feeds = feedRepository.findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();

        return feeds.stream()
                .limit(validatedSize)
                .map(FeedSummaryResponse::from)
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
     * 상단 고정된 것 우선, 최신순 정렬
     * 
     * @param togetherId Together ID
     * @return 공지 피드 목록
     */
    public List<FeedSummaryResponse> getTogetherNoticeFeeds(Long togetherId) {
        List<Feed> feeds = feedRepository.findByTogether_IdAndFeedTypeAndDeletedAtIsNullOrderByIsPinnedDescCreatedAtDesc(
                togetherId,
                com.back.domain.feed.entity.FeedType.TOGETHER_NOTICE
        );

        return feeds.stream()
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 Together의 상단 고정된 공지 피드만 조회
     * 
     * @param togetherId Together ID
     * @return 상단 고정된 공지 피드 목록
     */
    public List<FeedSummaryResponse> getPinnedNoticeFeeds(Long togetherId) {
        List<Feed> feeds = feedRepository.findByTogether_IdAndFeedTypeAndIsPinnedTrueAndDeletedAtIsNullOrderByCreatedAtDesc(
                togetherId,
                com.back.domain.feed.entity.FeedType.TOGETHER_NOTICE
        );

        return feeds.stream()
                .map(FeedSummaryResponse::from)
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
     * 무한 스크롤 응답 생성 (공통 로직)
     */
    private InfiniteScrollResponse<FeedSummaryResponse> createInfiniteScrollResponse(
            List<Feed> feeds,
            int requestedSize
    ) {
        // hasNext 계산: 요청한 개수보다 많이 조회되면 다음 페이지 존재
        boolean hasNext = feeds.size() > requestedSize;

        // 실제 반환할 데이터는 요청한 size만큼만
        List<Feed> actualFeeds = hasNext ? feeds.subList(0, requestedSize) : feeds;

        // DTO 변환
        List<FeedSummaryResponse> responses = actualFeeds.stream()
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());

        // nextCursor: 마지막 아이템의 ID (없으면 null)
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
