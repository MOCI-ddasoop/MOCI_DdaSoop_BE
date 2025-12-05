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
     * 특정 피드에 대한 특정 회원의 리액션 삭제
     * 사용 예: 회원이 좋아요 취소 버튼을 누를 때
     * @param feedId 피드 ID
     * @param memberId 회원 ID
     */
    void deleteByFeedIdAndMemberId(Long feedId, Long memberId);

    /**
     * 특정 피드에 달린 모든 회원의 리액션 삭제
     * 사용 예: 피드가 삭제될 때, 해당 피드에 좋아요를 누른 모든 사람의 리액션도 함께 제거
     * @param feedId 피드 ID
     * 예시: 피드100에 회원1,2,3이 좋아요 → deleteByFeedId(100) → 3개 모두 삭제
     */
    void deleteByFeedId(Long feedId);

    /**
     * 특정 회원이 누른 모든 피드의 리액션 삭제
     * 사용 예: 회원 탈퇴 시, 해당 회원이 누른 모든 좋아요 제거
     * @param memberId 회원 ID
     * 예시: 회원1이 피드100,200,300에 좋아요 → deleteByMemberId(1) → 3개 모두 삭제
     */
    void deleteByMemberId(Long memberId);
}
