package com.back.domain.member.controller;

import com.back.domain.member.dto.response.LastLoginProviderResponse;
import com.back.domain.member.dto.response.LoginResponse;
import com.back.domain.member.service.AuthService;
import com.back.global.util.CookieUtil;
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

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

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
        LoginResponse loginResponse = authService.login(memberId, response);
        
        // Access Token은 헤더로 전달 (클라이언트가 Authorization 헤더에서 읽어야 함)
        // 실제로는 클라이언트가 소셜 로그인 후 받은 정보로 토큰을 발급받으므로
        // 여기서는 예시로만 구현 (실제로는 소셜 로그인 콜백에서 처리)
        
        return ResponseEntity.ok(loginResponse);
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
                "Refresh Token은 쿠키에서 자동으로 읽어옵니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "토큰 갱신 성공",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAccessToken(
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

        // 응답에 Access Token 포함
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("accessToken", newAccessToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken)
                .body(responseBody);
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
}

