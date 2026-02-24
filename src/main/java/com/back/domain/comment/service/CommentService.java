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
import com.back.domain.notification.entity.NotificationTargetType;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.service.NotificationService;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final NotificationService notificationService;

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

        // 알림 발송
        if (parent == null) {
            // 최상위 댓글: 피드/함께하기 작성자에게 알림
            if (comment.isFeedComment() && comment.getFeed() != null) {
                notificationService.createNotification(
                        comment.getFeed().getMember().getId(),
                        currentMemberId,
                        NotificationType.FEED_COMMENT,
                        NotificationTargetType.FEED,
                        comment.getFeed().getId()
                );
            }
        } else {
            // 대댓글: 부모 댓글 작성자에게 알림
            notificationService.createNotification(
                    parent.getMember().getId(),
                    currentMemberId,
                    NotificationType.FEED_COMMENT_REPLY,
                    NotificationTargetType.COMMENT,
                    parent.getId()
            );
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

        Set<Long> reactedCommentIds = extractReactedCommentIds(List.of(comment), currentMemberId);

        return CommentResponse.from(comment, reactedCommentIds);
    }

    /**
     * Feed의 댓글 목록 조회 (페이징)
     */
    public Page<CommentResponse> getFeedComments(Long feedId, int page, int size, Long currentMemberId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByFeedIdAndParentIsNullAndDeletedAtIsNull(feedId, pageable);

        // 배치 조회: 최상위 댓글 + 대댓글 ID를 한 번에 수집
        Set<Long> reactedCommentIds = extractReactedCommentIds(comments.getContent(), currentMemberId);

        return comments.map(comment -> CommentResponse.from(comment, reactedCommentIds));
    }

    /**
     * Feed의 댓글 목록 조회 (전체 - 대댓글 포함)
     */
    public List<CommentResponse> getFeedCommentsAll(Long feedId, Long currentMemberId) {
        List<Comment> comments = commentRepository
                .findByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(feedId);

        // 배치 조회: 최상위 댓글 + 대댓글 ID를 한 번에 수집
        Set<Long> reactedCommentIds = extractReactedCommentIds(comments, currentMemberId);

        return comments.stream()
                .map(comment -> CommentResponse.from(comment, reactedCommentIds))
                .collect(Collectors.toList());
    }

    /**
     * Together의 댓글 목록 조회 (페이징)
     */
    public Page<CommentResponse> getTogetherComments(Long togetherId, int page, int size, Long currentMemberId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByTogetherIdAndParentIsNullAndDeletedAtIsNull(togetherId, pageable);

        // 배치 조회: 최상위 댓글 + 대댓글 ID를 한 번에 수집
        Set<Long> reactedCommentIds = extractReactedCommentIds(comments.getContent(), currentMemberId);

        return comments.map(comment -> CommentResponse.from(comment, reactedCommentIds));
    }

    /**
     * 대댓글 목록 조회
     */
    public List<CommentResponse> getReplies(Long parentId, Long currentMemberId) {
        List<Comment> replies = commentRepository.findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentId);

        // 배치 조회 (대댓글은 자식이 없으므로 replies만 수집)
        Set<Long> reactedCommentIds = extractReactedCommentIds(replies, currentMemberId);

        return replies.stream()
                .map(reply -> CommentResponse.fromWithoutReplies(reply, reactedCommentIds.contains(reply.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 인기 댓글 조회 (Feed)
     */
    public List<CommentResponse> getPopularFeedComments(Long feedId, int size, Long currentMemberId) {
        Pageable pageable = PageRequest.of(0, size);
        List<Comment> comments = commentRepository.findPopularCommentsByFeedId(feedId, pageable);

        Set<Long> reactedCommentIds = extractReactedCommentIds(comments, currentMemberId);

        return comments.stream()
                .map(comment -> CommentResponse.fromWithoutReplies(comment, reactedCommentIds.contains(comment.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 최신 댓글 조회 (Feed)
     */
    public List<CommentResponse> getRecentFeedComments(Long feedId, Long currentMemberId) {
        List<Comment> comments = commentRepository
                .findTop10ByFeedIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(feedId);

        Set<Long> reactedCommentIds = extractReactedCommentIds(comments, currentMemberId);

        return comments.stream()
                .map(comment -> CommentResponse.fromWithoutReplies(comment, reactedCommentIds.contains(comment.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 특정 회원이 작성한 댓글 조회
     */
    public Page<CommentResponse> getMemberComments(Long memberId, int page, int size, Long currentMemberId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId, pageable);

        Set<Long> reactedCommentIds = extractReactedCommentIds(comments.getContent(), currentMemberId);

        return comments.map(comment -> CommentResponse.fromWithoutReplies(comment, reactedCommentIds.contains(comment.getId())));
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
    public com.back.domain.comment.dto.response.CommentReactionResponse toggleReaction(Long commentId, Long currentMemberId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        boolean exists = commentReactionRepository.existsByCommentIdAndMemberId(commentId, currentMemberId);

        boolean isReacted;
        if (exists) {
            // 리액션 취소
            commentReactionRepository.deleteByCommentIdAndMemberId(commentId, currentMemberId);
            log.info("댓글 리액션 취소 - 댓글 ID: {}, 회원 ID: {}", commentId, currentMemberId);
            isReacted = false;
        } else {
            // 리액션 생성
            CommentReaction reaction = CommentReaction.builder()
                    .comment(comment)
                    .member(member)
                    .build();
            commentReactionRepository.save(reaction);
            log.info("댓글 리액션 생성 - 댓글 ID: {}, 회원 ID: {}", commentId, currentMemberId);

            // 알림 발송 (댓글 작성자에게)
            notificationService.createNotification(
                    comment.getMember().getId(),
                    currentMemberId,
                    NotificationType.COMMENT_REACTION,
                    NotificationTargetType.COMMENT,
                    commentId
            );

            isReacted = true;
        }

        // 최신 reactionCount 조회
        Comment updatedComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        return com.back.domain.comment.dto.response.CommentReactionResponse.of(isReacted, updatedComment.getReactionCount());
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

    // ========== Private 헬퍼 메서드 ==========

    /**
     * 배치 조회로 현재 사용자가 리액션한 댓글 ID Set을 반환 (N+1 방지)
     *
     * 최상위 댓글 목록을 받아서 대댓글(replies)까지 포함한 전체 ID를 수집한 뒤,
     * 쿼리 1번으로 현재 사용자가 리액션한 ID만 필터링해 반환한다.
     *
     * 비로그인 처리는 currentMemberId가 null이면 빈 Set을 반환하는 식으로
     */
    private Set<Long> extractReactedCommentIds(List<Comment> topLevelComments, Long currentMemberId) {
        if (currentMemberId == null || topLevelComments.isEmpty()) {
            return Set.of();
        }

        // 최상위 댓글 ID + 대댓글 ID를 한 번에 수집
        List<Long> allCommentIds = topLevelComments.stream()
                .flatMap(comment -> Stream.concat(
                        Stream.of(comment.getId()),
                        comment.getReplies().stream()
                                .filter(reply -> !reply.isDeleted())
                                .map(Comment::getId)
                ))
                .collect(Collectors.toList());

        // 배치 조회 (쿼리 1번)
        return new HashSet<>(
                commentReactionRepository.findReactedCommentIdsByMemberIdAndCommentIdIn(
                        currentMemberId, allCommentIds
                )
        );
    }

}
