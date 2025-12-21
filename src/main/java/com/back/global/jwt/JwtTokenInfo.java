package com.back.global.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** JWT 토큰에서 추출한 정보 DTO */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenInfo {
    
    private Long memberId;
    private String email;
    private String role;
}


