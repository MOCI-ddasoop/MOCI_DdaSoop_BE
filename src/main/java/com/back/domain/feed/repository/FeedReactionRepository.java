package com.back.domain.feed.repository;

import com.back.domain.feed.entity.FeedReaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

    /**
     * 특정 피드에 특정 회원이 리액션을 눌렀는지 확인
     */
    Optional<FeedReaction> findByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 피드의 리액션 여부 확인
     */
    boolean existsByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 피드의 리액션 개수
     */
    Long countByFeedId(Long feedId);

    /**
     * 특정 회원이 리액션한 피드 목록 (최신순)
     */
    @Query("SELECT fr.feed FROM FeedReaction fr WHERE fr.member.id = :memberId " +
            "AND fr.feed.deletedAt IS NULL ORDER BY fr.createdAt DESC")
    List<Object> findFeedsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 회원이 리액션한 피드 ID 목록
     */
    @Query("SELECT fr.feed.id FROM FeedReaction fr WHERE fr.member.id = :memberId")
    List<Long> findFeedIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 피드의 모든 리액션 삭제
     */
    void deleteByFeedId(Long feedId);

    /**
     * 특정 회원의 모든 리액션 삭제
     */
    void deleteByMemberId(Long memberId);
}
