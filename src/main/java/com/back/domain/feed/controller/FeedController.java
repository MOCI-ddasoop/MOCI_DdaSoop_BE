package com.back.domain.feed.controller;

import com.back.domain.feed.dto.feed.request.FeedCreateRequest;
import com.back.domain.feed.dto.feed.request.FeedSearchRequest;
import com.back.domain.feed.dto.feed.request.FeedUpdateRequest;
import com.back.domain.feed.dto.feed.response.FeedResponse;
import com.back.domain.feed.dto.feed.response.FeedSummaryResponse;
import com.back.domain.feed.dto.feed.response.InfiniteScrollResponse;
import com.back.domain.feed.service.FeedService;
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

@Tag(name = "Feed", description = "피드 API")
@Slf4j
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(
            summary = "피드 생성",
            description = "새로운 피드를 생성합니다. content와 tags는 별도로 입력받으며, 최대 10개의 이미지를 첨부할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "피드 생성 성공",
                    content = @Content(schema = @Schema(implementation = Long.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)")
    })
    @PostMapping
    public ResponseEntity<Long> createFeed(
            @Valid @RequestBody FeedCreateRequest request
            // @AuthenticationPrincipal CustomUserDetails userDetails  // 인증 후 추가
    ) {
        // Long currentMemberId = userDetails.getMemberId();  // 인증 후 사용
        Long currentMemberId = 1L;  // 임시 (Member 연결 전)

        Long feedId = feedService.createFeed(request, currentMemberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(feedId);
    }

    @Operation(
            summary = "피드 상세 조회",
            description = "특정 피드의 상세 정보를 조회합니다. 이미지, 태그, 카운트 정보를 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FeedResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeed(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId
            // @AuthenticationPrincipal CustomUserDetails userDetails  // 선택적 인증
    ) {
        // Long currentMemberId = userDetails != null ? userDetails.getMemberId() : null;
        Long currentMemberId = 1L;  // 임시

        FeedResponse response = feedService.getFeed(feedId, currentMemberId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "피드 목록 조회 (페이징 + QueryDSL 동적 검색)",
            description = "피드 목록을 페이징 방식으로 조회합니다. QueryDSL 동적 쿼리로 다양한 조건 조합이 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping
    public ResponseEntity<Page<FeedSummaryResponse>> getFeedList(
            @Parameter(description = "검색 및 필터 조건", required = false)
            @ModelAttribute FeedSearchRequest searchRequest
    ) {
        Page<FeedSummaryResponse> response = feedService.getFeedList(searchRequest);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "전체 피드 무한 스크롤",
            description = "커서 기반 페이징으로 전체 피드를 조회합니다. hasNext를 통해 다음 페이지 존재 여부를 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = InfiniteScrollResponse.class))
            )
    })
    @GetMapping("/scroll")
    public ResponseEntity<InfiniteScrollResponse<FeedSummaryResponse>> getFeedListInfiniteScroll(
            @Parameter(description = "마지막으로 조회한 피드 ID (첫 조회 시에는 생략)", required = false, example = "100")
            @RequestParam(required = false) Long lastFeedId,
            @Parameter(description = "조회할 개수 (기본 20, 최대 50)", required = false, example = "20")
            @RequestParam(required = false) Integer size
    ) {
        InfiniteScrollResponse<FeedSummaryResponse> response =
                feedService.getFeedListInfiniteScroll(lastFeedId, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 회원의 피드 무한 스크롤",
            description = "특정 회원이 작성한 피드를 무한 스크롤로 조회합니다. (프로필 페이지, 마이페이지용)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = InfiniteScrollResponse.class))
            )
    })
    @GetMapping("/members/{memberId}/scroll")
    public ResponseEntity<InfiniteScrollResponse<FeedSummaryResponse>> getMemberFeedsInfiniteScroll(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable Long memberId,
            @Parameter(description = "마지막으로 조회한 피드 ID", required = false, example = "100")
            @RequestParam(required = false) Long lastFeedId,
            @Parameter(description = "조회할 개수 (기본 20, 최대 50)", required = false, example = "20")
            @RequestParam(required = false) Integer size
    ) {
        InfiniteScrollResponse<FeedSummaryResponse> response =
                feedService.getMemberFeedsInfiniteScroll(memberId, lastFeedId, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 Together의 인증 피드 무한 스크롤",
            description = "특정 함께하기 모임의 인증 피드를 무한 스크롤로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = InfiniteScrollResponse.class))
            )
    })
    @GetMapping("/together/{togetherId}/scroll")
    public ResponseEntity<InfiniteScrollResponse<FeedSummaryResponse>> getTogetherFeedsInfiniteScroll(
            @Parameter(description = "함께하기 ID", required = true, example = "1")
            @PathVariable Long togetherId,
            @Parameter(description = "마지막으로 조회한 피드 ID", required = false, example = "100")
            @RequestParam(required = false) Long lastFeedId,
            @Parameter(description = "조회할 개수 (기본 20, 최대 50)", required = false, example = "20")
            @RequestParam(required = false) Integer size
    ) {
        InfiniteScrollResponse<FeedSummaryResponse> response =
                feedService.getTogetherFeedsInfiniteScroll(togetherId, lastFeedId, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "피드 수정",
            description = "기존 피드의 내용, 이미지, 태그, 공개 범위를 수정합니다. 작성자만 수정 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
            @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @PutMapping("/{feedId}")
    public ResponseEntity<Void> updateFeed(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId,
            @Valid @RequestBody FeedUpdateRequest request
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        feedService.updateFeed(feedId, request, currentMemberId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "피드 삭제",
            description = "피드를 논리적으로 삭제(Soft Delete)합니다. 작성자만 삭제 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
            @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        feedService.deleteFeed(feedId, currentMemberId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "피드 리액션 토글 (좋아요)",
            description = "피드에 좋아요를 추가하거나 취소합니다. 이미 좋아요를 누른 경우 취소되고, 아니면 추가됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토글 성공 (true: 좋아요 추가, false: 좋아요 취소)",
                    content = @Content(schema = @Schema(implementation = Boolean.class))
            ),
            @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @PostMapping("/{feedId}/reactions")
    public ResponseEntity<Boolean> toggleReaction(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        boolean isReacted = feedService.toggleReaction(feedId, currentMemberId);

        return ResponseEntity.ok(isReacted);
    }

    @Operation(
            summary = "피드 북마크 토글",
            description = "피드를 북마크에 추가하거나 제거합니다. 이미 북마크한 경우 제거되고, 아니면 추가됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토글 성공 (true: 북마크 추가, false: 북마크 제거)",
                    content = @Content(schema = @Schema(implementation = Boolean.class))
            ),
            @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @PostMapping("/{feedId}/bookmarks")
    public ResponseEntity<Boolean> toggleBookmark(
            @Parameter(description = "피드 ID", required = true, example = "1")
            @PathVariable Long feedId
            // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // 임시

        boolean isBookmarked = feedService.toggleBookmark(feedId, currentMemberId);

        return ResponseEntity.ok(isBookmarked);
    }

    @Operation(
            summary = "태그로 피드 검색",
            description = "특정 태그가 포함된 피드를 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/search/tag")
    public ResponseEntity<List<FeedSummaryResponse>> searchByTag(
            @Parameter(description = "검색할 태그", required = true, example = "여행")
            @RequestParam String tag,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        List<FeedSummaryResponse> response = feedService.searchByTag(tag, page, size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "인기 피드 조회 (최근 7일)",
            description = "리액션(좋아요)이 많은 인기 피드를 조회합니다. (최근 7일 기준)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/popular")
    public ResponseEntity<List<FeedSummaryResponse>> getPopularFeeds(
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        List<FeedSummaryResponse> response = feedService.getPopularFeeds(size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "댓글 많은 피드 Top N",
            description = "댓글이 가장 많은 피드를 조회합니다. (토론 많은 게시물)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/most-commented")
    public ResponseEntity<List<FeedSummaryResponse>> getMostCommentedFeeds(
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        List<FeedSummaryResponse> response = feedService.getMostCommentedFeeds(size);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "북마크 많은 피드 Top N",
            description = "북마크가 가장 많은 피드를 조회합니다. (가장 많이 저장된 게시물)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            )
    })
    @GetMapping("/most-bookmarked")
    public ResponseEntity<List<FeedSummaryResponse>> getMostBookmarkedFeeds(
            @Parameter(description = "조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        List<FeedSummaryResponse> response = feedService.getMostBookmarkedFeeds(size);

        return ResponseEntity.ok(response);
    }
}
