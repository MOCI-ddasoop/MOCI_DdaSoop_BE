package com.back.domain.member.controller;

import com.back.domain.member.dto.request.EmailCheckRequest;
import com.back.domain.member.dto.request.MemberUpdateRequest;
import com.back.domain.member.dto.request.MemberWithdrawRequest;
import com.back.domain.member.dto.request.NicknameCheckRequest;
import com.back.domain.member.dto.response.EmailCheckResponse;
import com.back.domain.member.dto.response.MemberInfoResponse;
import com.back.domain.member.dto.response.MemberWithdrawResponse;
import com.back.domain.member.dto.response.NicknameCheckResponse;
import com.back.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원 API")
@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(
        summary = "내 정보 조회",
        description = "현재 로그인한 회원의 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = MemberInfoResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<MemberInfoResponse> getMyInfo(
        // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // TODO: 인증 연결 후 userDetails.getMemberId() 사용

        MemberInfoResponse response = memberService.getMemberInfo(currentMemberId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "닉네임 중복 체크",
        description = "닉네임이 이미 사용 중인지 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "체크 완료",
            content = @Content(schema = @Schema(implementation = NicknameCheckResponse.class))
        )
    })
    @PostMapping("/check-nickname")
    public ResponseEntity<NicknameCheckResponse> checkNickname(
        @Valid @RequestBody NicknameCheckRequest request
    ) {
        boolean exists = memberService.checkNickname(request.getNickname());

        if (exists) {
            return ResponseEntity.ok(NicknameCheckResponse.unavailable(request.getNickname()));
        }

        return ResponseEntity.ok(NicknameCheckResponse.available(request.getNickname()));
    }

    @Operation(
        summary = "이메일 중복 체크",
        description = "이메일이 이미 사용 중인지 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "체크 완료",
            content = @Content(schema = @Schema(implementation = EmailCheckResponse.class))
        )
    })
    @PostMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(
        @Valid @RequestBody EmailCheckRequest request
    ) {
        boolean exists = memberService.checkEmail(request.getEmail());

        if (exists) {
            return ResponseEntity.ok(EmailCheckResponse.unavailable(request.getEmail()));
        }

        return ResponseEntity.ok(EmailCheckResponse.available(request.getEmail()));
    }

    @Operation(
        summary = "회원 정보 수정",
        description = "현재 로그인한 회원의 정보를 수정합니다. 이메일, 닉네임, 프로필 이미지를 수정할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "중복된 이메일 또는 닉네임")
    })
    @PutMapping("/me")
    public ResponseEntity<Void> updateMember(
        @Valid @RequestBody MemberUpdateRequest request
        // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // TODO: 인증 연결 후 userDetails.getMemberId() 사용

        memberService.updateMember(currentMemberId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "현재 로그인한 회원을 탈퇴 처리합니다. (Soft Delete)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "탈퇴 성공",
            content = @Content(schema = @Schema(implementation = MemberWithdrawResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @DeleteMapping("/me")
    public ResponseEntity<MemberWithdrawResponse> withdrawMember(
        @RequestBody(required = false) MemberWithdrawRequest request
        // @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentMemberId = 1L;  // TODO: 인증 연결 후 userDetails.getMemberId() 사용
        String reason = request != null ? request.getReason() : null;

        memberService.withdrawMember(currentMemberId, reason);
        return ResponseEntity.ok(MemberWithdrawResponse.success());
    }
}

