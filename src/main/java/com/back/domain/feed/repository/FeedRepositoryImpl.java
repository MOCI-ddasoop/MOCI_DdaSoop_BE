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
    public List<Feed> findPopularFeedsWithCondition(FeedSearchCondition condition, int limit, Long currentMemberId) {
        QFeed feed = QFeed.feed;

        BooleanBuilder builder = createBaseCondition(condition);
        builder.and(createVisibilityCondition(feed, currentMemberId));

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
    public List<Feed> findFeedsForInfiniteScroll(Long cursorId, int limit, Long currentMemberId) {
        QFeed feed = QFeed.feed;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(feed.id.lt(cursorId));
        builder.and(feed.deletedAt.isNull());
        builder.and(createVisibilityCondition(feed, currentMemberId));

        return queryFactory
                .selectFrom(feed)
                .where(builder)
                .orderBy(feed.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> findMemberFeedsForInfiniteScroll(Long memberId, Long cursorId, int limit, Long currentMemberId) {
        QFeed feed = QFeed.feed;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(feed.member.id.eq(memberId));
        builder.and(feed.id.lt(cursorId));
        builder.and(feed.deletedAt.isNull());
        builder.and(createVisibilityCondition(feed, currentMemberId));

        return queryFactory
                .selectFrom(feed)
                .where(builder)
                .orderBy(feed.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> findTogetherFeedsForInfiniteScroll(Long togetherId, Long cursorId, int limit, boolean isFirstPage, Long currentMemberId) {
        QFeed feed = QFeed.feed;

        List<Feed> result = new java.util.ArrayList<>();

        // 첫 페이지일 때만 핀 고정 피드 포함
        if (isFirstPage) {
            com.querydsl.core.types.dsl.NumberExpression<Integer> priority =
                new com.querydsl.core.types.dsl.CaseBuilder()
                    .when(feed.feedType.eq(com.back.domain.feed.entity.FeedType.TOGETHER_NOTICE)).then(3)
                    .when(feed.feedType.eq(com.back.domain.feed.entity.FeedType.TOGETHER_VERIFICATION)).then(2)
                    .otherwise(0);

            BooleanBuilder pinnedBuilder = new BooleanBuilder();
            pinnedBuilder.and(feed.together.id.eq(togetherId));
            pinnedBuilder.and(feed.isPinned.isTrue());
            pinnedBuilder.and(feed.deletedAt.isNull());
            pinnedBuilder.and(createTogetherVisibilityCondition(feed, togetherId, currentMemberId));

            List<Feed> pinnedFeeds = queryFactory
                    .selectFrom(feed)
                    .where(pinnedBuilder)
                    .orderBy(
                        priority.desc(),
                        feed.pinOrder.asc().nullsLast()
                    )
                    .fetch();

            result.addAll(pinnedFeeds);
            limit = Math.max(limit - pinnedFeeds.size(), 1);
        }

        BooleanBuilder unpinnedBuilder = new BooleanBuilder();
        unpinnedBuilder.and(feed.together.id.eq(togetherId));
        unpinnedBuilder.and(feed.isPinned.isFalse());
        unpinnedBuilder.and(feed.id.lt(cursorId));
        unpinnedBuilder.and(feed.deletedAt.isNull());
        unpinnedBuilder.and(createTogetherVisibilityCondition(feed, togetherId, currentMemberId));

        List<Feed> unpinnedFeeds = queryFactory
                .selectFrom(feed)
                .where(unpinnedBuilder)
                .orderBy(feed.createdAt.desc())
                .limit(limit)
                .fetch();

        result.addAll(unpinnedFeeds);
        return result;
    }

    @Override
    public List<Feed> findByTagForInfiniteScroll(String tag, Long cursorId, int limit, Long currentMemberId) {
        QFeed feed = QFeed.feed;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(feed.tags.any().eq(tag));
        builder.and(feed.id.lt(cursorId));
        builder.and(feed.deletedAt.isNull());
        builder.and(createVisibilityCondition(feed, currentMemberId));

        return queryFactory
                .selectFrom(feed)
                .where(builder)
                .orderBy(feed.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> findRecommendedFeedsByTags(List<String> tags, Long excludeMemberId, List<Long> excludeFeedIds, int limit, Long currentMemberId) {
        QFeed feed = QFeed.feed;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(feed.deletedAt.isNull());
        builder.and(createVisibilityCondition(feed, currentMemberId));

        // 추천 태그 중 하나라도 포함된 피드
        if (tags != null && !tags.isEmpty()) {
            builder.and(feed.tags.any().in(tags));
        }

        // 본인이 작성한 피드 제외
        if (excludeMemberId != null) {
            builder.and(feed.member.id.ne(excludeMemberId));
        }

        // 이미 조회된 피드 제외
        if (excludeFeedIds != null && !excludeFeedIds.isEmpty()) {
            builder.and(feed.id.notIn(excludeFeedIds));
        }

        return queryFactory
                .selectFrom(feed)
                .where(builder)
                .orderBy(
                    feed.reactionCount.desc(),  // 인기도 우선
                    feed.createdAt.desc()       // 최신순
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> findFeedsForInfiniteScrollExcluding(Long cursorId, List<Long> excludeFeedIds, int limit, Long currentMemberId) {
        QFeed feed = QFeed.feed;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(feed.id.lt(cursorId));
        builder.and(feed.deletedAt.isNull());
        builder.and(createVisibilityCondition(feed, currentMemberId));

        // 제외할 피드 ID 목록
        if (excludeFeedIds != null && !excludeFeedIds.isEmpty()) {
            builder.and(feed.id.notIn(excludeFeedIds));
        }

        return queryFactory
                .selectFrom(feed)
                .where(builder)
                .orderBy(feed.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Feed> searchFeedsForInfiniteScroll(FeedSearchCondition condition, Long cursorId, int limit) {
        QFeed feed = QFeed.feed;

        // 1. 동적 조건 생성
        BooleanBuilder builder = createBaseCondition(condition);
        
        // 2. 커서 조건 추가
        builder.and(feed.id.lt(cursorId));

        // 3. Feed ID만 먼저 조회 (동적 조건 적용, 중복 없음)
        List<Long> feedIds = queryFactory
                .select(feed.id)
                .from(feed)
                .where(builder)
                .orderBy(getOrderSpecifier(condition.getSortBy()))
                .limit(limit)
                .fetch();

        // Feed가 없으면 빈 리스트 반환
        if (feedIds.isEmpty()) {
            return List.of();
        }

        // 4. ID로 Feed + 연관 엔티티 Fetch Join (조건 고정, N+1 방지)
        List<Feed> content = queryFactory
                .selectFrom(feed)
                .distinct()
                .leftJoin(feed.member).fetchJoin()        // Member Fetch Join (N+1 방지)
                .leftJoin(feed.images).fetchJoin()        // FeedImage Fetch Join (N+1 방지)
                .leftJoin(feed.together).fetchJoin()      // Together Fetch Join (N+1 방지)
                .where(feed.id.in(feedIds))               // ID IN 쿼리 (중복 최소화)
                .orderBy(getOrderSpecifier(condition.getSortBy()))
                .fetch();

        return content;
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
     * Visibility 접근 권한 조건 생성
     *
     * - PUBLIC    : 모든 사람
     * - FOLLOWERS : 팔로우 기능 미구현 → PUBLIC과 동일하게 처리
     * - PRIVATE   : 작성자 본인만
     * - MEMBERS   : 해당 Together에 PARTICIPATING 상태로 참여 중인 멤버 + 작성자 본인
     *
     * 비로그인(currentMemberId == null)이면 PUBLIC / FOLLOWERS 만 노출
     */
    private com.querydsl.core.types.Predicate createVisibilityCondition(QFeed feed, Long currentMemberId) {
        com.querydsl.core.types.dsl.BooleanExpression isPublicOrFollowers =
                feed.visibility.eq(com.back.domain.feed.entity.FeedVisibility.PUBLIC)
                .or(feed.visibility.eq(com.back.domain.feed.entity.FeedVisibility.FOLLOWERS));

        if (currentMemberId == null) {
            // 비로그인: PUBLIC / FOLLOWERS 만
            return isPublicOrFollowers;
        }

        com.querydsl.core.types.dsl.BooleanExpression isOwner =
                feed.member.id.eq(currentMemberId);

        // PRIVATE: 본인만
        com.querydsl.core.types.dsl.BooleanExpression privateCondition =
                feed.visibility.eq(com.back.domain.feed.entity.FeedVisibility.PRIVATE)
                .and(isOwner);

        // MEMBERS: 본인이거나, 해당 together에 PARTICIPATING 상태로 참여 중인 경우
        com.back.domain.together.entity.QParticipants qParticipants =
                com.back.domain.together.entity.QParticipants.participants;

        com.querydsl.jpa.JPAExpressions.selectOne();
        com.querydsl.core.types.dsl.BooleanExpression isMember =
                com.querydsl.jpa.JPAExpressions
                        .selectOne()
                        .from(qParticipants)
                        .where(
                                qParticipants.together.id.eq(feed.together.id)
                                .and(qParticipants.member.id.eq(currentMemberId))
                                .and(qParticipants.participantsStatus.eq(
                                        com.back.domain.together.entity.ParticipantsStatus.PARTICIPATING))
                        )
                        .exists();

        com.querydsl.core.types.dsl.BooleanExpression membersOrNoticeCondition =
                feed.visibility.in(
                        com.back.domain.feed.entity.FeedVisibility.MEMBERS,
                        com.back.domain.feed.entity.FeedVisibility.NOTICE
                )
                .and(isOwner.or(
                        feed.together.isNotNull().and(isMember)
                ));

        return isPublicOrFollowers
                .or(privateCondition)
                .or(membersOrNoticeCondition);
    }

    /**
     * Together 피드 전용 Visibility 조건 생성
     * togetherId가 이미 고정된 컨텍스트에서 사용 — MEMBERS 서브쿼리에 feed.together.id 대신 togetherId 직접 사용
     */
    private com.querydsl.core.types.Predicate createTogetherVisibilityCondition(QFeed feed, Long togetherId, Long currentMemberId) {
        com.querydsl.core.types.dsl.BooleanExpression isPublicOrFollowers =
                feed.visibility.eq(com.back.domain.feed.entity.FeedVisibility.PUBLIC)
                .or(feed.visibility.eq(com.back.domain.feed.entity.FeedVisibility.FOLLOWERS));

        if (currentMemberId == null) {
            return isPublicOrFollowers;
        }

        com.querydsl.core.types.dsl.BooleanExpression isOwner =
                feed.member.id.eq(currentMemberId);

        com.querydsl.core.types.dsl.BooleanExpression privateCondition =
                feed.visibility.eq(com.back.domain.feed.entity.FeedVisibility.PRIVATE)
                .and(isOwner);

        // MEMBERS, NOTICE: togetherId를 직접 사용하여 서브쿼리 JOIN 문제 해결
        com.back.domain.together.entity.QParticipants qParticipants =
                com.back.domain.together.entity.QParticipants.participants;

        com.querydsl.core.types.dsl.BooleanExpression isMember =
                com.querydsl.jpa.JPAExpressions
                        .selectOne()
                        .from(qParticipants)
                        .where(
                                qParticipants.together.id.eq(togetherId)
                                .and(qParticipants.member.id.eq(currentMemberId))
                                .and(qParticipants.participantsStatus.eq(
                                        com.back.domain.together.entity.ParticipantsStatus.PARTICIPATING))
                        )
                        .exists();

        com.querydsl.core.types.dsl.BooleanExpression membersOrNoticeCondition =
                feed.visibility.in(
                        com.back.domain.feed.entity.FeedVisibility.MEMBERS,
                        com.back.domain.feed.entity.FeedVisibility.NOTICE
                )
                .and(isOwner.or(isMember));

        return isPublicOrFollowers
                .or(privateCondition)
                .or(membersOrNoticeCondition);
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
