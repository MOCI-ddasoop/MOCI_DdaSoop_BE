package com.back.domain.report.dto.request;

import com.back.domain.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 처리 요청 DTO (관리자용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportProcessRequest {

    /**
     * 처리 상태 (APPROVED 또는 REJECTED)
     */
    @NotNull(message = "처리 상태는 필수입니다.")
    private ReportStatus status;

    /**
     * 관리자 코멘트
     * 최대 1000자
     */
    @NotNull(message = "관리자 코멘트는 필수입니다.")
    @Size(min = 1, max = 1000, message = "관리자 코멘트는 1~1000자 이내로 입력해주세요.")
    private String adminComment;
}
