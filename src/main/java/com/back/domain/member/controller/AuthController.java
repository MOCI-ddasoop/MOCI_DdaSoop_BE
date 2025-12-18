package com.back.domain.member.controller;

import com.back.domain.member.dto.response.LastLoginProviderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Operation(
        summary = "최근 로그인 방식 조회",
        description = "세션에 저장된 최근 로그인 방식을 조회합니다. 로그인하지 않은 상태에서도 이전 로그인 기록이 있으면 표시됩니다."
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
        HttpSession session
    ) {
        String provider = (String) session.getAttribute("lastLoginProvider");

        if (provider != null) {
            return ResponseEntity.ok(LastLoginProviderResponse.from(provider));
        }

        return ResponseEntity.ok(LastLoginProviderResponse.empty());
    }
}

