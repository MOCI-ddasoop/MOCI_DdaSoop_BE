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

        // hasNext 판단을 위해 21개 조회
        List<Feed> feeds = feedRepository
                .findTop21ByIdLessThanAndDeletedAtIsNullOrderByIdDesc(cursorId);

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

        List<Feed> feeds = feedRepository
                .findTop21ByMemberIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(memberId, cursorId);

        return createInfiniteScrollResponse(feeds, requestedSize);
    }

    /**
     * 특정 Together의 인증 피드 무한 스크롤 (커서 기반)
     */
    public InfiniteScrollResponse<FeedSummaryResponse> getTogetherFeedsInfiniteScroll(
            Long togetherId,
            Long lastFeedId,
            Integer size
    ) {
        int requestedSize = (size != null && size > 0 && size <= 50) ? size : 20;
        Long cursorId = lastFeedId != null ? lastFeedId : Long.MAX_VALUE;

        List<Feed> feeds = feedRepository
                .findTop21ByTogetherIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(togetherId, cursorId);

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
     * 태그로 피드 검색 (QueryDSL)
     */
    public List<FeedSummaryResponse> searchByTag(String tag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<String> tags = List.of(tag);

        List<Feed> feeds = feedRepository.findByTagsWithDynamicQuery(tags, pageable);

        return feeds.stream()
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 인기 피드 조회 (최근 7일 기준, QueryDSL)
     */
    public List<FeedSummaryResponse> getPopularFeeds(int size) {
        FeedSearchCondition condition = FeedSearchCondition.builder()
                .startDate(LocalDateTime.now().minusDays(7))  // 최근 7일
                .build();

        List<Feed> feeds = feedRepository.findPopularFeedsWithCondition(condition, size);

        return feeds.stream()
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 많은 피드 Top N
     */
    public List<FeedSummaryResponse> getMostCommentedFeeds(int size) {
        List<Feed> feeds = feedRepository.findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();

        return feeds.stream()
                .limit(size)
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 북마크 많은 피드 Top N
     */
    public List<FeedSummaryResponse> getMostBookmarkedFeeds(int size) {
        List<Feed> feeds = feedRepository.findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();

        return feeds.stream()
                .limit(size)
                .map(FeedSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 회원이 북마크한 피드 목록 조회 (페이징)
     */
    public Page<FeedSummaryResponse> getBookmarkedFeeds(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feed> feeds = feedBookmarkRepository.findBookmarkedFeedsByMemberId(memberId, pageable);

        return feeds.map(FeedSummaryResponse::from);
    }

    /**
     * 특정 회원의 북마크 개수
     */
    public Long getBookmarkCount(Long memberId) {
        return feedBookmarkRepository.countByMemberId(memberId);
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
}
