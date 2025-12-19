package com.back.domain.comment.service;

import com.back.domain.comment.dto.request.CommentCreateRequest;
import com.back.domain.comment.dto.request.CommentUpdateRequest;
import com.back.domain.comment.dto.response.CommentResponse;
import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.entity.CommentReaction;
import com.back.domain.comment.entity.CommentType;
import com.back.domain.comment.repository.CommentReactionRepository;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.together.entity.Together;
import com.back.domain.together.repository.TogetherRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final FeedRepository feedRepository;
    private final TogetherRepository togetherRepository;
    private final MemberRepository memberRepository;

    /**
     * 댓글 생성
     */
    @Transactional
    public Long createComment(CommentCreateRequest request, Long currentMemberId) {
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        // 부모 댓글 조회 (대댓글인 경우)
        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findByIdAndDeletedAtIsNull(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
        }

        // CommentType에 따라 대상 엔티티 조회 및 Comment 생성
        Comment comment = switch (request.getCommentType()) {
            case FEED -> {
                Feed feed = feedRepository.findByIdAndDeletedAtIsNull(request.getTargetId())
                        .orElseThrow(() -> new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage()));
                
                yield Comment.builder()
                        .commentType(CommentType.FEED)
                        .content(request.getContent())
                        .member(member)
                        .feed(feed)
                        .parent(parent)
                        .build();
            }
            case TOGETHER -> {
                Together together = togetherRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new IllegalArgumentException("함께하기를 찾을 수 없습니다."));
                
                yield Comment.builder()
                        .commentType(CommentType.TOGETHER)
                        .content(request.getContent())
                        .member(member)
                        .together(together)
                        .parent(parent)
                        .build();
            }
            case DONATION -> throw new IllegalArgumentException("DONATION 타입은 아직 지원되지 않습니다.");
        };

        Comment savedComment = commentRepository.save(comment);

        // Feed 댓글 개수 증가
        if (comment.isFeedComment()) {
            comment.notifyFeedCommentCreated();
        }

        log.info("댓글 생성 완료 - ID: {}, Type: {}, TargetId: {}", 
                savedComment.getId(), request.getCommentType(), request.getTargetId());

        return savedComment.getId();
    }

    /**
     * 댓글 상세 조회
     */
    public CommentResponse getComment(Long commentId, Long currentMemberId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 현재 사용자의 리액션 여부 확인
        boolean isReacted = currentMemberId != null &&
                commentReactionRepository.existsByCommentIdAndMemberId(commentId, currentMemberId);

        return CommentResponse.from(comment, isReacted);
    }

    /**
     * Feed의 댓글 목록 조회 (페이징)
     */
    public Page<CommentResponse> getFeedComments(Long feedId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByFeedIdAndParentIsNullAndDeletedAtIsNull(feedId, pageable);

        return comments.map(CommentResponse::from);
    }

    /**
     * Feed의 댓글 목록 조회 (전체 - 대댓글 포함)
     */
    public List<CommentResponse> getFeedCommentsAll(Long feedId) {
        List<Comment> comments = commentRepository
                .findByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(feedId);

        return comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Together의 댓글 목록 조회 (페이징)
     */
    public Page<CommentResponse> getTogetherComments(Long togetherId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByTogetherIdAndParentIsNullAndDeletedAtIsNull(togetherId, pageable);

        return comments.map(CommentResponse::from);
    }

    /**
     * 대댓글 목록 조회
     */
    public List<CommentResponse> getReplies(Long parentId) {
        List<Comment> replies = commentRepository.findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentId);

        return replies.stream()
                .map(CommentResponse::fromWithoutReplies)
                .collect(Collectors.toList());
    }

    /**
     * 인기 댓글 조회 (Feed)
     */
    public List<CommentResponse> getPopularFeedComments(Long feedId, int size) {
        Pageable pageable = PageRequest.of(0, size);
        List<Comment> comments = commentRepository.findPopularCommentsByFeedId(feedId, pageable);

        return comments.stream()
                .map(CommentResponse::fromWithoutReplies)
                .collect(Collectors.toList());
    }

    /**
     * 최신 댓글 조회 (Feed)
     */
    public List<CommentResponse> getRecentFeedComments(Long feedId) {
        List<Comment> comments = commentRepository
                .findTop10ByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(feedId);

        return comments.stream()
                .map(CommentResponse::fromWithoutReplies)
                .collect(Collectors.toList());
    }

    /**
     * 특정 회원이 작성한 댓글 조회
     */
    public Page<CommentResponse> getMemberComments(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId, pageable);

        return comments.map(CommentResponse::fromWithoutReplies);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public void updateComment(Long commentId, CommentUpdateRequest request, Long currentMemberId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 체크 (작성자만 수정 가능)
        if (!comment.getMember().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }

        comment.updateContent(request.getContent());

        log.info("댓글 수정 완료 - ID: {}", commentId);
    }

    /**
     * 댓글 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteComment(Long commentId, Long currentMemberId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 체크
        if (!comment.getMember().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }

        comment.delete();

        // Feed 댓글 개수 감소
        if (comment.isFeedComment()) {
            comment.notifyFeedCommentDeleted();
        }

        log.info("댓글 삭제 완료 - ID: {}", commentId);
    }

    /**
     * 댓글 리액션 토글 (좋아요)
     */
    @Transactional
    public boolean toggleReaction(Long commentId, Long currentMemberId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        boolean exists = commentReactionRepository.existsByCommentIdAndMemberId(commentId, currentMemberId);

        if (exists) {
            // 리액션 취소
            commentReactionRepository.deleteByCommentIdAndMemberId(commentId, currentMemberId);
            comment.decrementReactionCount();
            log.info("댓글 리액션 취소 - 댓글 ID: {}, 회원 ID: {}", commentId, currentMemberId);
            return false;
        } else {
            // 리액션 생성
            CommentReaction reaction = CommentReaction.builder()
                    .comment(comment)
                    .member(member)
                    .build();
            commentReactionRepository.save(reaction);
            comment.incrementReactionCount();
            log.info("댓글 리액션 생성 - 댓글 ID: {}, 회원 ID: {}", commentId, currentMemberId);
            return true;
        }
    }

    /**
     * Feed의 댓글 개수
     */
    public Long getFeedCommentCount(Long feedId) {
        return commentRepository.countByFeedId(feedId);
    }

    /**
     * Together의 댓글 개수
     */
    public Long getTogetherCommentCount(Long togetherId) {
        return commentRepository.countByTogetherId(togetherId);
    }
}
