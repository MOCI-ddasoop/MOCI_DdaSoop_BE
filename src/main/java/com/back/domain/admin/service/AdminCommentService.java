package com.back.domain.admin.service;

import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.repository.CommentRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 댓글 제어 서비스.
 * CommentRepository만 사용 (comment 도메인 코드 수정 없음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final CommentRepository commentRepository;

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
}
