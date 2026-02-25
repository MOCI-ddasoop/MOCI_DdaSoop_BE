package com.back.domain.admin.controller;

import com.back.domain.admin.dto.request.AdminMemberRoleUpdateRequest;
import com.back.domain.admin.dto.response.AdminMemberDetailResponse;
import com.back.domain.admin.dto.response.AdminMemberSummaryResponse;
import com.back.domain.admin.dto.response.DashboardStatsResponse;
import com.back.domain.admin.service.AdminCommentService;
import com.back.domain.admin.service.AdminDashboardService;
import com.back.domain.admin.service.AdminFeedService;
import com.back.domain.admin.service.AdminMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin", description = "관리자 API (대시보드, 회원, 피드, 댓글 관리)")
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminMemberService adminMemberService;
    private final AdminFeedService adminFeedService;
    private final AdminCommentService adminCommentService;

    // ========== 대시보드 ==========

    @Operation(summary = "관리자 API 진입 확인", description = "관리자 API 사용 가능 여부를 확인합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "정상 진입")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> adminEntry() {
        log.debug("Admin domain entry: GET /api/admin");
        return ResponseEntity.ok(Map.of(
                "admin", true,
                "message", "Admin API entry"
        ));
    }

    @Operation(summary = "대시보드 통계 조회", description = "관리자 대시보드용 통계를 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = DashboardStatsResponse.class))
        )
    })
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = adminDashboardService.getStats();
        return ResponseEntity.ok(stats);
    }

    // ========== 회원 ==========

    @Operation(summary = "회원 목록 페이징", description = "탈퇴 회원 포함 회원 목록을 페이징 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AdminMemberSummaryResponse.class))
        )
    })
    @GetMapping("/members")
    public ResponseEntity<Page<AdminMemberSummaryResponse>> getMemberPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminMemberSummaryResponse> result = adminMemberService.getMemberPage(pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "회원 상세 조회", description = "회원 상세 정보를 조회합니다. 탈퇴 회원도 조회 가능합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AdminMemberDetailResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping("/members/{memberId}")
    public ResponseEntity<AdminMemberDetailResponse> getMemberDetail(@PathVariable Long memberId) {
        AdminMemberDetailResponse detail = adminMemberService.getMemberDetail(memberId);
        return ResponseEntity.ok(detail);
    }

    @Operation(summary = "회원 역할 변경", description = "회원 역할을 USER 또는 ADMIN으로 변경합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "변경 성공",
            content = @Content(schema = @Schema(implementation = AdminMemberDetailResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PutMapping("/members/{memberId}/role")
    public ResponseEntity<AdminMemberDetailResponse> updateMemberRole(
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberRoleUpdateRequest request
    ) {
        AdminMemberDetailResponse updated = adminMemberService.updateRole(memberId, request.getRole());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "회원 탈퇴 처리", description = "회원을 탈퇴 처리합니다. (Soft Delete, 프로필 익명화)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "탈퇴 처리 성공"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> softDeleteMember(@PathVariable Long memberId) {
        adminMemberService.softDeleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 복구", description = "탈퇴 처리된 회원을 복구합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "복구 성공",
            content = @Content(schema = @Schema(implementation = AdminMemberDetailResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PutMapping("/members/{memberId}/restore")
    public ResponseEntity<AdminMemberDetailResponse> restoreMember(@PathVariable Long memberId) {
        AdminMemberDetailResponse restored = adminMemberService.restoreMember(memberId);
        return ResponseEntity.ok(restored);
    }

    // ========== 피드 ==========

    @Operation(summary = "피드 강제 삭제", description = "피드를 강제 삭제합니다. (Soft Delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @DeleteMapping("/feeds/{feedId}")
    public ResponseEntity<Void> forceDeleteFeed(@PathVariable Long feedId) {
        adminFeedService.forceDeleteFeed(feedId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "피드 비공개 처리", description = "피드를 비공개로 변경합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "처리 성공"),
        @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @PutMapping("/feeds/{feedId}/visibility/private")
    public ResponseEntity<Void> setFeedPrivate(@PathVariable Long feedId) {
        adminFeedService.setFeedVisibilityPrivate(feedId);
        return ResponseEntity.noContent().build();
    }

    // ========== 댓글 ==========

    @Operation(summary = "댓글 강제 삭제", description = "댓글을 강제 삭제합니다. (Soft Delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> forceDeleteComment(@PathVariable Long commentId) {
        adminCommentService.forceDeleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
