package com.back.domain.admin.service;

import com.back.domain.admin.dto.response.AdminCommentSummaryResponse;
import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.entity.CommentType;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.report.entity.ReportTargetType;
import com.back.domain.report.service.ReportService;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 댓글 제어/조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final CommentRepository commentRepository;
    private final ReportService reportService;

    /**
     * 댓글 강제 삭제 (soft delete)
     */
    @Transactional
    public void forceDeleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.COMMENT_NOT_FOUND.getMessage()));
        if (comment.isDeleted()) {
            throw new IllegalArgumentException(ErrorCode.COMMENT_ALREADY_DELETED.getMessage());
        }
        comment.delete();
        commentRepository.save(comment);
        log.info("관리자 댓글 강제 삭제 - commentId: {}", commentId);
    }

    /**
     * 관리자용 댓글 리스트 조회 (필터 + 페이징).
     * - commentType: FEED/TOGETHER/DONATION 필터
     * - authorId: 작성자 필터
     * - reportedOnly: 신고가 1건 이상 있는 댓글만
     */
    @Transactional(readOnly = true)
    public Page<AdminCommentSummaryResponse> getCommentPageForAdmin(
            CommentType commentType,
            Long authorId,
            Boolean reportedOnly,
            Pageable pageable
    ) {
        boolean reportFilter = Boolean.TRUE.equals(reportedOnly);
        Page<Comment> comments = commentRepository.findAdminComments(commentType, authorId, reportFilter, pageable);

        return comments.map(comment -> {
            Long reportCount = reportService.getReportCount(ReportTargetType.COMMENT, comment.getId());
            return AdminCommentSummaryResponse.from(comment, reportCount);
        });
    }
}

