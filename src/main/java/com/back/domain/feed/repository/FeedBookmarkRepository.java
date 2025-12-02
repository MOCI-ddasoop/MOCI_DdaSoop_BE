package com.back.domain.feed.repository;

import com.back.domain.feed.entity.FeedBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedBookmarkRepository extends JpaRepository<FeedBookmark, Long> {

    /**
     * 특정 피드에 특정 회원이 북마크했는지 확인
     */
    Optional<FeedBookmark> findByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 피드의 북마크 여부 확인
     */
    boolean existsByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 피드의 북마크 개수
     */
    Long countByFeedId(Long feedId);

    /**
     * 특정 회원이 북마크한 피드 목록 (페이징)
     */
    @Query("SELECT fb.feed FROM FeedBookmark fb WHERE fb.member.id = :memberId " +
            "AND fb.feed.deletedAt IS NULL ORDER BY fb.createdAt DESC")
    Page<Object> findFeedsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 회원이 북마크한 피드 목록 (무한 스크롤)
     */
    @Query("SELECT fb.feed FROM FeedBookmark fb WHERE fb.member.id = :memberId " +
            "AND fb.id < :lastBookmarkId AND fb.feed.deletedAt IS NULL " +
            "ORDER BY fb.createdAt DESC")
    List<Object> findFeedsByMemberIdWithCursor(
            @Param("memberId") Long memberId,
            @Param("lastBookmarkId") Long lastBookmarkId,
            Pageable pageable
    );

    /**
     * 특정 회원이 북마크한 피드 ID 목록
     */
    @Query("SELECT fb.feed.id FROM FeedBookmark fb WHERE fb.member.id = :memberId")
    List<Long> findFeedIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원의 북마크 개수
     */
    Long countByMemberId(Long memberId);

    /**
     * 특정 피드의 모든 북마크 삭제
     */
    void deleteByFeedId(Long feedId);

    /**
     * 특정 회원의 모든 북마크 삭제
     */
    void deleteByMemberId(Long memberId);
}
