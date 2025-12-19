package com.back.domain.feed.repository;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * FeedBookmark Repository ( Spring Data JPA 기반)
 */
public interface FeedBookmarkRepository extends JpaRepository<FeedBookmark, Long> {

    /**
     * 특정 회원이 특정 피드를 북마크했는지 확인
     */
    boolean existsByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 회원의 특정 피드 북마크 삭제
     */
    void deleteByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 회원이 북마크한 피드 목록 조회 (페이징)
     */
    @Query("SELECT fb.feed FROM FeedBookmark fb " +
           "WHERE fb.member.id = :memberId AND fb.feed.deletedAt IS NULL " +
           "ORDER BY fb.createdAt DESC")
    Page<Feed> findBookmarkedFeedsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 회원이 북마크한 피드 개수 조회
     */
    Long countByMemberId(Long memberId);
}
