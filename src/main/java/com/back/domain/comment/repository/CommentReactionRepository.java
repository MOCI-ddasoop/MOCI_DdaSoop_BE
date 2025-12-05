package com.back.domain.comment.repository;

import com.back.domain.comment.entity.CommentReaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    /**
     * 특정 댓글에 특정 회원이 리액션을 눌렀는지 확인
     */
    Optional<CommentReaction> findByCommentIdAndMemberId(Long commentId, Long memberId);

    /**
     * 특정 댓글의 리액션 여부 확인
     */
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    /**
     * 특정 댓글의 리액션 개수
     */
    Long countByCommentId(Long commentId);

    /**
     * 특정 회원이 리액션한 댓글 목록 (최신순)
     */
    @Query("SELECT cr.comment FROM CommentReaction cr WHERE cr.member.id = :memberId " +
            "AND cr.comment.deletedAt IS NULL ORDER BY cr.createdAt DESC")
    List<Object> findCommentsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 특정 회원이 리액션한 댓글 ID 목록
     */
    @Query("SELECT cr.comment.id FROM CommentReaction cr WHERE cr.member.id = :memberId")
    List<Long> findCommentIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 댓글의 모든 리액션 삭제
     */
    void deleteByCommentId(Long commentId);

    /**
     * 특정 회원의 모든 리액션 삭제
     */
    void deleteByMemberId(Long memberId);

    /**
     * 특정 피드의 모든 댓글 리액션 삭제
     */
    @Query("DELETE FROM CommentReaction cr WHERE cr.comment.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);
}
