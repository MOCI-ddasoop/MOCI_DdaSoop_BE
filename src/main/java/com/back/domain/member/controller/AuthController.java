package com.back.domain.member.controller;

import com.back.domain.member.dto.request.AdditionalInfoRequest;
import com.back.domain.member.dto.response.LastLoginProviderResponse;
import com.back.domain.member.dto.response.LoginResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.AuthService;
import com.back.domain.member.service.MemberService;
import com.back.global.exception.ErrorCode;
import com.back.global.jwt.JwtTokenProvider;
import com.back.global.util.CookieUtil;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;
    private final CookieUtil cookieUtil;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
        summary = "로그인",
        description = "회원 ID를 받아 Access Token과 Refresh Token을 발급합니다. " +
                "Access Token은 Authorization 헤더로 반환되며, Refresh Token은 쿠키로 저장됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestParam Long memberId,
            HttpServletResponse response
    ) {
        // memberId 유효성 검증
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException(com.back.global.exception.ErrorCode.INVALID_INPUT_VALUE.getMessage());
        }

        String accessToken = authService.login(memberId, response);
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage()
                ));
        LoginResponse loginResponse = LoginResponse.from(member);
        
        // Access Token은 헤더로 전달 (보편적 구조: AT는 Header, RT는 Cookie)
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(loginResponse);
    }

    @Operation(
        summary = "로그아웃",
        description = "현재 로그인한 회원의 Refresh Token을 무효화하고 쿠키에서 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long memberId,
            HttpServletResponse response
    ) {
        authService.logout(memberId, response);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Access Token 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. " +
                "Refresh Token은 쿠키에서 자동으로 읽어옵니다. " +
                "Access Token은 Authorization 헤더로 반환됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "토큰 갱신 성공 (Access Token은 Authorization 헤더에 포함됨)"
        ),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshAccessToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 쿠키에서 Refresh Token 추출
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);
        
        if (refreshToken == null) {
            throw new IllegalArgumentException(
                    com.back.global.exception.ErrorCode.AUTH_TOKEN_INVALID.getMessage()
            );
        }

        // 새로운 Access Token 발급
        String newAccessToken = authService.refreshAccessToken(refreshToken);

        // Access Token은 헤더로만 전달 (보편적 구조: AT는 Header, RT는 Cookie)
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken)
                .build();
    }

    @Operation(
        summary = "최근 로그인 방식 조회",
        description = "쿠키에 저장된 최근 로그인 방식을 조회합니다. 로그인하지 않은 상태에서도 이전 로그인 기록이 있으면 표시됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = LastLoginProviderResponse.class))
        )
    })
    @GetMapping("/last-login-provider")
    public ResponseEntity<LastLoginProviderResponse> getLastLoginProvider(
        HttpServletRequest request
    ) {
        String provider = cookieUtil.getLastLoginProviderFromCookie(request);

        if (provider != null) {
            return ResponseEntity.ok(LastLoginProviderResponse.from(provider));
        }

        return ResponseEntity.ok(LastLoginProviderResponse.empty());
    }

    @Operation(
        summary = "추가 정보 입력 완료",
        description = "소셜 로그인 후 필수 정보(닉네임, 이메일)를 입력하여 회원가입을 완료합니다. " +
                "회원 상태가 확정된 후에만 JWT 토큰이 발급됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "회원가입 완료",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패, 중복된 닉네임/이메일)"),
        @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PostMapping("/complete-registration")
    public ResponseEntity<LoginResponse> completeRegistration(
            @Valid @RequestBody AdditionalInfoRequest request,
            HttpServletResponse response
    ) {
        // 임시 토큰 검증 및 memberId 추출
        Long memberId;
        try {
            memberId = jwtTokenProvider.validateAndGetMemberIdFromTemporaryToken(
                    request.getTemporaryToken()
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                com.back.global.exception.ErrorCode.AUTH_TOKEN_INVALID.getMessage()
            );
        }
        
        // 추가 정보 입력 완료 처리
        memberService.completeAdditionalInfo(memberId, request.getNickname(), request.getEmail());

        // 추가 정보 입력 완료 후 회원 조회
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.MEMBER_NOT_FOUND.getMessage()
                ));

        // 추가 정보가 실제로 입력되었는지 확인 (null이나 blank 값이 들어온 경우 방지)
        if (member.isAdditionalInfoRequired()) {
            throw new IllegalArgumentException(
                com.back.global.exception.ErrorCode.INVALID_INPUT_VALUE.getMessage()
            );
        }

        // 로그인 처리 (JWT 토큰 발급) - 회원 상태 확정 시에만 JWT 발급
        String accessToken = authService.login(memberId, response);
        LoginResponse loginResponse = LoginResponse.from(member);

        // Access Token은 헤더로 전달 (보편적 구조: AT는 Header, RT는 Cookie)
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(loginResponse);
    }
}

