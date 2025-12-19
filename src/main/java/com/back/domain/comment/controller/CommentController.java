package com.back.domain.comment.controller;

import com.back.domain.comment.dto.request.CommentCreateRequest;
import com.back.domain.comment.dto.request.CommentUpdateRequest;
import com.back.domain.comment.dto.response.CommentResponse;
import com.back.domain.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment", description = "댓글 API")
@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 생성",
            description = "새로운 댓글을 생성합니다. commentType에 따라 Feed, Together, Donation 댓글을 구분합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "댓글 생성 성공",
                    content = @Content(schema = @Schema(implementation = Long.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)")
    })
    @PostMapping
    public ResponseEntity<Long> createComment(
            @Valid @RequestBody CommentCreateRequest request
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        Long commentId = commentService.createComment(request, currentMemberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(commentId);
    }

    @Operation(
            summary = "댓글 상세 조회",
            description = "특정 댓글의 상세 정보를 조회합니다. 대댓글 목록도 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getComment(
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        CommentResponse response = commentService.getComment(commentId, currentMemberId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Feed의 댓글 목록 조회 (페이징)",
            description = "특정 피드의 최상위 댓글 목록을 페이징 방식으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping("/feeds/{feedId}")
    public ResponseEntity<Page<CommentResponse>> getFeedComments(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentResponse> response = commentService.getFeedComments(feedId, page, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Feed의 댓글 목록 조회 (전체 - 대댓글 포함)",
            description = "특정 피드의 모든 댓글을 조회합니다. 각 댓글의 대댓글도 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/feeds/{feedId}/all")
    public ResponseEntity<List<CommentResponse>> getFeedCommentsAll(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId
    ) {
        List<CommentResponse> response = commentService.getFeedCommentsAll(feedId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Together의 댓글 목록 조회 (페이징)",
            description = "특정 함께하기의 최상위 댓글 목록을 페이징 방식으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping("/together/{togetherId}")
    public ResponseEntity<Page<CommentResponse>> getTogetherComments(
            @Parameter(description = "함께하기 ID", required = true, example = "1")
            @PathVariable Long togetherId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentResponse> response = commentService.getTogetherComments(togetherId, page, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "대댓글 목록 조회",
            description = "특정 댓글의 대댓글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getReplies(
            @Parameter(description = "부모 댓글 ID", required = true, example = "1")
            @PathVariable Long commentId
    ) {
        List<CommentResponse> response = commentService.getReplies(commentId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Feed의 인기 댓글 조회",
            description = "특정 피드의 인기 댓글(리액션 많은 순)을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/feeds/{feedId}/popular")
    public ResponseEntity<List<CommentResponse>> getPopularFeedComments(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId,
            @Parameter(description = "조회할 개수", example = "5")
            @RequestParam(defaultValue = "5") int size
    ) {
        List<CommentResponse> response = commentService.getPopularFeedComments(feedId, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Feed의 최신 댓글 조회",
            description = "특정 피드의 최신 댓글을 조회합니다. (최대 10개)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/feeds/{feedId}/recent")
    public ResponseEntity<List<CommentResponse>> getRecentFeedComments(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId
    ) {
        List<CommentResponse> response = commentService.getRecentFeedComments(feedId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 회원이 작성한 댓글 조회",
            description = "특정 회원이 작성한 모든 댓글을 페이징 방식으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping("/members/{memberId}")
    public ResponseEntity<Page<CommentResponse>> getMemberComments(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable Long memberId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommentResponse> response = commentService.getMemberComments(memberId, page, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "댓글 수정",
            description = "기존 댓글의 내용을 수정합니다. 작성자만 수정 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        commentService.updateComment(commentId, request, currentMemberId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 논리적으로 삭제(Soft Delete)합니다. 작성자만 삭제 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        commentService.deleteComment(commentId, currentMemberId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "댓글 리액션 토글 (좋아요)",
            description = "댓글에 좋아요를 추가하거나 취소합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토글 성공 (true: 좋아요 추가, false: 좋아요 취소)",
                    content = @Content(schema = @Schema(implementation = Boolean.class))
            ),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PostMapping("/{commentId}/reactions")
    public ResponseEntity<Boolean> toggleReaction(
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        boolean isReacted = commentService.toggleReaction(commentId, currentMemberId);

        return ResponseEntity.ok(isReacted);
    }

    @Operation(
            summary = "Feed의 댓글 개수 조회",
            description = "특정 피드의 전체 댓글 개수를 조회합니다. (대댓글 포함)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Long.class))
            )
    })
    @GetMapping("/feeds/{feedId}/count")
    public ResponseEntity<Long> getFeedCommentCount(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId
    ) {
        Long count = commentService.getFeedCommentCount(feedId);

        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Together의 댓글 개수 조회",
            description = "특정 함께하기의 전체 댓글 개수를 조회합니다. (대댓글 포함)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Long.class))
            )
    })
    @GetMapping("/together/{togetherId}/count")
    public ResponseEntity<Long> getTogetherCommentCount(
            @Parameter(description = "함께하기 ID", required = true, example = "1")
            @PathVariable Long togetherId
    ) {
        Long count = commentService.getTogetherCommentCount(togetherId);

        return ResponseEntity.ok(count);
    }
}
