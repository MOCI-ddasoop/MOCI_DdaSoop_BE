package com.back.domain.comment.repository;

import com.back.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ========== 기본 조회 ==========

    /**
     * ID로 삭제되지 않은 댓글 조회
     */
    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    // ========== 피드의 댓글 조회 ==========

    /**
     * 특정 피드의 최상위 댓글만 조회 (삭제된 것 제외, 최신순)
     */
    List<Comment> findByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(Long feedId);

    /**
     * 특정 피드의 최상위 댓글 조회 (페이징)
     */
    Page<Comment> findByFeedIdAndParentIsNullAndDeletedAtIsNull(Long feedId, Pageable pageable);

    /**
     * 특정 피드의 전체 댓글 개수 (대댓글 포함, 삭제된 것 제외)
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.feed.id = :feedId AND c.deletedAt IS NULL")
    Long countByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 피드의 최상위 댓글 개수만
     */
    Long countByFeedIdAndParentIsNullAndDeletedAtIsNull(Long feedId);

    // ========== 대댓글 조회 ==========

    /**
     * 특정 댓글의 대댓글 조회 (최신순)
     */
    List<Comment> findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentId);

    /**
     * 특정 댓글의 대댓글 개수
     */
    Long countByParentIdAndDeletedAtIsNull(Long parentId);

    // ========== 회원의 댓글 ==========

    /**
     * 특정 회원이 작성한 댓글 조회 (페이징)
     */
    Page<Comment> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 특정 회원의 댓글 개수
     */
    Long countByMemberIdAndDeletedAtIsNull(Long memberId);

    /**
     * 특정 회원이 특정 피드에 작성한 댓글 목록
     */
    List<Comment> findByFeedIdAndMemberIdAndDeletedAtIsNull(Long feedId, Long memberId);

    // ========== 인기 댓글 ==========

    /**
     * 특정 피드의 인기 댓글 (리액션 많은 순)
     */
    @Query("SELECT c FROM Comment c WHERE c.feed.id = :feedId AND c.parent IS NULL " +
            "AND c.deletedAt IS NULL ORDER BY c.reactionCount DESC, c.createdAt ASC")
    List<Comment> findPopularCommentsByFeedId(@Param("feedId") Long feedId, Pageable pageable);

    /**
     * 특정 피드의 최신 댓글 N개
     */
    List<Comment> findTop10ByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long feedId);

    // ========== 삭제 관련 ==========

    /**
     * 특정 피드의 모든 댓글 삭제 (Soft Delete)
     */
    @Query("UPDATE Comment c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.feed.id = :feedId")
    void softDeleteByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 댓글과 그 대댓글 모두 삭제 (Soft Delete)
     */
    @Query("UPDATE Comment c SET c.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE c.id = :commentId OR c.parent.id = :commentId")
    void softDeleteCommentAndReplies(@Param("commentId") Long commentId);

    // ========== 검색 ==========

    /**
     * 댓글 내용으로 검색
     */
    @Query("SELECT c FROM Comment c WHERE c.content LIKE %:keyword% AND c.deletedAt IS NULL")
    List<Comment> searchByContent(@Param("keyword") String keyword, Pageable pageable);
}
