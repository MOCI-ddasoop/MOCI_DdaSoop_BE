package com.back.domain.comment.repository;

import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.entity.CommentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * 관리자용 댓글 목록 조회 (필터 + 페이징, 최신순)
     */
    @Query(
        value = """
            SELECT c FROM Comment c
            WHERE c.deletedAt IS NULL
              AND (:commentType IS NULL OR c.commentType = :commentType)
              AND (:authorId IS NULL OR c.member.id = :authorId)
              AND (:reportedOnly = false OR EXISTS (
                    SELECT 1 FROM Report r
                    WHERE r.targetType = com.back.domain.report.entity.ReportTargetType.COMMENT
                      AND r.targetId = c.id
              ))
            ORDER BY c.createdAt DESC
            """,
        countQuery = """
            SELECT COUNT(c) FROM Comment c
            WHERE c.deletedAt IS NULL
              AND (:commentType IS NULL OR c.commentType = :commentType)
              AND (:authorId IS NULL OR c.member.id = :authorId)
              AND (:reportedOnly = false OR EXISTS (
                    SELECT 1 FROM Report r
                    WHERE r.targetType = com.back.domain.report.entity.ReportTargetType.COMMENT
                      AND r.targetId = c.id
              ))
            """
    )
    Page<Comment> findAdminComments(
            @Param("commentType") CommentType commentType,
            @Param("authorId") Long authorId,
            @Param("reportedOnly") boolean reportedOnly,
            Pageable pageable
    );

    // ========== 피드의 댓글 조회 ==========

    /**
     * 특정 피드의 최상위 댓글만 조회 (삭제된 것 제외, 최신순)
     */
    List<Comment> findByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long feedId);

    /**
     * 특정 피드의 최상위 댓글 조회 (페이징, 최신순)
     * @EntityGraph로 replies fetch join하여 N+1 방지
     */
    @EntityGraph(attributePaths = {"replies", "member"})
    @Query("SELECT c FROM Comment c " +
           "WHERE c.feed.id = :feedId AND c.parent IS NULL AND c.deletedAt IS NULL " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findByFeedIdAndParentIsNullAndDeletedAtIsNull(
        @Param("feedId") Long feedId, 
        Pageable pageable
    );

    /**
     * 특정 피드의 전체 댓글 개수 (대댓글 포함, 삭제된 것 제외)
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.feed.id = :feedId AND c.deletedAt IS NULL")
    Long countByFeedId(@Param("feedId") Long feedId);

    // ========== 대댓글 조회 ==========

    /**
     * 특정 댓글의 대댓글 조회 (삭제된 것 제외, 오래된 순)
     */
    List<Comment> findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentId);

    // ========== 회원의 댓글 ==========

    /**
     * 특정 회원이 작성한 댓글 조회 (페이징)
     */
    Page<Comment> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 특정 회원의 댓글 개수
     */
    Long countByMemberIdAndDeletedAtIsNull(Long memberId);

    // ========== 인기 댓글 ==========

    /**
     * 특정 피드의 인기 댓글 (리액션 많은 순)
     */
    @Query("SELECT c FROM Comment c WHERE c.feed.id = :feedId AND c.parent IS NULL " +
            "AND c.deletedAt IS NULL ORDER BY c.reactionCount DESC, c.createdAt DESC")
    List<Comment> findPopularCommentsByFeedId(@Param("feedId") Long feedId, Pageable pageable);

    /**
     * 특정 피드의 최신 댓글 N개
     */
    List<Comment> findTop10ByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long feedId);

    // ========== Together의 댓글 조회 ==========

    /**
     * 특정 Together의 최상위 댓글 조회 (페이징, 최신순)
     * @EntityGraph로 replies fetch join하여 N+1 방지
     */
    @EntityGraph(attributePaths = {"replies", "member"})
    @Query("SELECT c FROM Comment c " +
           "WHERE c.together.id = :togetherId AND c.parent IS NULL AND c.deletedAt IS NULL " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findByTogetherIdAndParentIsNullAndDeletedAtIsNull(
        @Param("togetherId") Long togetherId,
        Pageable pageable
    );

    /**
     * 특정 Together의 전체 댓글 개수 (대댓글 포함)
     */
    Long countByTogetherId(Long togetherId);

}
