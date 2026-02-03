package com.back.domain.feed.repository;

import com.back.domain.feed.dto.feed.request.FeedSearchCondition;
import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.QFeed;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Feed QueryDSL Custom Repository 구현체
 * 복잡한 동적 쿼리를 QueryDSL로 구현
 */
@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Feed> searchFeeds(FeedSearchCondition condition, Pageable pageable) {
        QFeed feed = QFeed.feed;

        // 1. 동적 조건 생성
        BooleanBuilder builder = createBaseCondition(condition);

        // 2. Feed ID만 먼저 조회 (동적 조건 적용, 중복 없음)
        List<Long> feedIds = queryFactory
                .select(feed.id)
                .from(feed)
                .where(builder)
                .orderBy(getOrderSpecifier(condition.getSortBy()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Feed가 없으면 빈 페이지 반환
        if (feedIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }

        // 3. ID로 Feed + 연관 엔티티 Fetch Join (조건 고정, N+1 방지)
        List<Feed> content = queryFactory
                .selectFrom(feed)
                .distinct()  // IN 쿼리에서는 중복 거의 없지만 안전을 위해 유지
                .leftJoin(feed.member).fetchJoin()        // Member Fetch Join (N+1 방지)
                .leftJoin(feed.images).fetchJoin()        // FeedImage Fetch Join (N+1 방지)
                .leftJoin(feed.together).fetchJoin()      // Together Fetch Join (N+1 방지)
                .where(feed.id.in(feedIds))               // ID IN 쿼리 (중복 최소화)
                .orderBy(getOrderSpecifier(condition.getSortBy()))
                .fetch();

        // 4. 전체 개수 조회 (Total Count)
        Long total = queryFactory
                .select(feed.count())
                .from(feed)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // ========== Public 메서드 ==========

    @Override
    public List<Feed> findPopularFeedsWithCondition(FeedSearchCondition condition, int limit) {
        QFeed feed = QFeed.feed;
        
        BooleanBuilder builder = createBaseCondition(condition);
        
        return queryFactory
                .selectFrom(feed)
                .where(builder)
                .orderBy(feed.reactionCount.desc(), feed.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Long countByCondition(FeedSearchCondition condition) {
        QFeed feed = QFeed.feed;
        
        BooleanBuilder builder = createBaseCondition(condition);
        
        return queryFactory
                .select(feed.count())
                .from(feed)
                .where(builder)
                .fetchOne();
    }

    @Override
    public List<Feed> findFeedsForInfiniteScroll(Long cursorId, int limit) {
        QFeed feed = QFeed.feed;
        
        return queryFactory
                .selectFrom(feed)
                .where(
                    feed.id.lt(cursorId)
                    .and(feed.deletedAt.isNull())
                )
                .orderBy(feed.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> findMemberFeedsForInfiniteScroll(Long memberId, Long cursorId, int limit) {
        QFeed feed = QFeed.feed;
        
        return queryFactory
                .selectFrom(feed)
                .where(
                    feed.member.id.eq(memberId)
                    .and(feed.id.lt(cursorId))
                    .and(feed.deletedAt.isNull())
                )
                .orderBy(feed.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> findTogetherFeedsForInfiniteScroll(Long togetherId, Long cursorId, int limit) {
        QFeed feed = QFeed.feed;
        
        // 우선순위 계산:
        // 3: 핀 고정 + 공지
        // 2: 핀 고정 + 인증
        // 0: 핀 미고정
        com.querydsl.core.types.dsl.NumberExpression<Integer> priority = 
            new com.querydsl.core.types.dsl.CaseBuilder()
                .when(feed.isPinned.isTrue().and(feed.feedType.eq(com.back.domain.feed.entity.FeedType.TOGETHER_NOTICE)))
                .then(3)
                .when(feed.isPinned.isTrue().and(feed.feedType.eq(com.back.domain.feed.entity.FeedType.TOGETHER_VERIFICATION)))
                .then(2)
                .otherwise(0);
        
        return queryFactory
                .selectFrom(feed)
                .where(
                    feed.together.id.eq(togetherId)
                    .and(feed.id.lt(cursorId))
                    .and(feed.deletedAt.isNull())
                )
                .orderBy(
                    priority.desc(),              // 1순위: 우선순위 (핀 공지 > 핀 인증 > 일반)
                    feed.pinOrder.asc().nullsLast(),  // 2순위: 핀 순서 (1, 2, 3, ...)
                    feed.createdAt.desc()         // 3순위: 최신순
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> findByTagForInfiniteScroll(String tag, Long cursorId, int limit) {
        QFeed feed = QFeed.feed;
        
        return queryFactory
                .selectFrom(feed)
                .where(
                    feed.tags.any().eq(tag)
                    .and(feed.id.lt(cursorId))
                    .and(feed.deletedAt.isNull())
                )
                .orderBy(feed.id.desc())
                .limit(limit)
                .fetch();
    }

    // ========== Private 헬퍼 메서드 ==========

    /**
     * 기본 조건 생성 (공통 로직)
     * 모든 검색 메서드에서 사용하는 공통 조건을 생성
     */
    private BooleanBuilder createBaseCondition(FeedSearchCondition condition) {
        QFeed feed = QFeed.feed;
        BooleanBuilder builder = new BooleanBuilder();
        
        // 1. 삭제되지 않은 피드만 (필수 조건)
        builder.and(feed.deletedAt.isNull());
        
        // 2. 피드 타입 조건
        if (condition.getFeedType() != null) {
            builder.and(feed.feedType.eq(condition.getFeedType()));
        }
        
        // 3. 작성자 조건
        if (condition.getMemberId() != null) {
            builder.and(feed.member.id.eq(condition.getMemberId()));
        }
        
        // 4. 태그 조건 (OR)
        if (condition.getTags() != null && !condition.getTags().isEmpty()) {
            builder.and(feed.tags.any().in(condition.getTags()));
        }
        
        // 5. 키워드 검색 (내용)
        if (condition.getKeyword() != null && !condition.getKeyword().isEmpty()) {
            builder.and(feed.content.containsIgnoreCase(condition.getKeyword()));
        }
        
        // 6. 공개 범위 조건
        if (condition.getVisibility() != null) {
            builder.and(feed.visibility.eq(condition.getVisibility()));
        }
        
        // 7. 함께하기 조건
        if (condition.getTogetherId() != null) {
            builder.and(feed.together.id.eq(condition.getTogetherId()));
        }
        
        // 8. 기간 조건 (시작일)
        if (condition.getStartDate() != null) {
            builder.and(feed.createdAt.goe(condition.getStartDate()));
        }
        
        // 9. 기간 조건 (종료일)
        if (condition.getEndDate() != null) {
            builder.and(feed.createdAt.loe(condition.getEndDate()));
        }
        
        return builder;
    }

    /**
     * 정렬 조건 생성
     * sortBy 값에 따라 적절한 OrderSpecifier 반환
     */
    private OrderSpecifier<?> getOrderSpecifier(String sortBy) {
        QFeed feed = QFeed.feed;
        
        if (sortBy == null) {
            return feed.createdAt.desc();  // 기본: 최신순
        }
        
        return switch (sortBy) {
            case "popular" -> feed.reactionCount.desc();      // 인기순 (리액션 많은 순)
            case "comments" -> feed.commentCount.desc();      // 댓글순 (댓글 많은 순)
            case "bookmarks" -> feed.bookmarkCount.desc();    // 북마크순 (북마크 많은 순)
            default -> feed.createdAt.desc();                 // 최신순 (기본값)
        };
    }
}
