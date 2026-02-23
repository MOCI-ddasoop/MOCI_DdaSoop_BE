package com.back.domain.comment.repository;

import com.back.domain.comment.entity.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    /**
     * 특정 댓글의 리액션 여부 확인
     */
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    /**
     * 특정 댓글의 특정 회원 리액션 삭제
     */
    void deleteByCommentIdAndMemberId(Long commentId, Long memberId);

    /**
     * 특정 회원이 리액션한 댓글 ID 목록 (배치 조회 - N+1 방지)
     * 댓글 ID 목록 중 현재 사용자가 리액션한 것만 필터링해서 반환
     */
    @Query("SELECT cr.comment.id FROM CommentReaction cr " +
            "WHERE cr.member.id = :memberId AND cr.comment.id IN :commentIds")
    List<Long> findReactedCommentIdsByMemberIdAndCommentIdIn(
            @Param("memberId") Long memberId,
            @Param("commentIds") List<Long> commentIds
    );
}
