package com.back.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 신고 사유 타입
 * 사용자가 신고 시 선택할 수 있는 사유 목록
 */
@Getter
@RequiredArgsConstructor
public enum ReportReasonType {
    
    // ========== 공통 사유 ==========
    SPAM("스팸 또는 광고"),
    HATE_SPEECH("혐오 발언"),
    HARASSMENT("괴롭힘 또는 악의적 행위"),
    INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
    VIOLENCE("폭력적 콘텐츠"),
    FALSE_INFORMATION("허위 정보"),
    COPYRIGHT("저작권 침해"),
    PRIVACY("개인정보 노출"),
    
    // ========== 기타 ==========
    OTHER("기타");

    private final String description;
}
