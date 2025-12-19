package com.back.domain.feed.repository;

import com.back.domain.feed.entity.FeedReaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * FeedReaction Repository
 */
public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

    /**
     * 특정 회원이 특정 피드에 좋아요를 눌렀는지 확인
     * - FeedService.toggleReaction() - 좋아요 토글 전 확인 용
     * - FeedResponse 생성 시 isReacted 설정
     */
    boolean existsByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 회원의 특정 피드 좋아요 삭제
     */
    void deleteByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 피드에 좋아요를 누른 회원 목록 조회 (페이징)
     */
    Page<FeedReaction> findByFeedIdOrderByCreatedAtDesc(Long feedId, Pageable pageable);

    /**
     * 특정 피드의 리액션 개수
     * - 통계 확인 (Feed.reactionCount와 동기화 체크하기 위해)
     */
    Long countByFeedId(Long feedId);

    /**
     * 특정 회원이 좋아요를 누른 피드 목록 조회 (페이징)
     */
    @Query("SELECT fr.feed FROM FeedReaction fr " +
           "WHERE fr.member.id = :memberId AND fr.feed.deletedAt IS NULL " +
           "ORDER BY fr.createdAt DESC")
    Page<FeedReaction> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId, Pageable pageable);
}
