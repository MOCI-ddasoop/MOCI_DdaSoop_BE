package com.back.domain.report.dto.request;

import com.back.domain.report.entity.ReportReasonType;
import com.back.domain.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequest {

    /**
     * 신고 대상 타입 (FEED, COMMENT, TOGETHER)
     */
    @NotNull(message = "신고 대상 타입은 필수입니다.")
    private ReportTargetType targetType;

    /**
     * 신고 대상 ID
     */
    @NotNull(message = "신고 대상 ID는 필수입니다.")
    private Long targetId;

    /**
     * 신고 사유 타입
     */
    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReasonType reasonType;

    /**
     * 상세 사유 (기타 선택 시 필수)
     * 최대 1000자
     */
    @Size(max = 1000, message = "상세 사유는 최대 1000자까지 입력 가능합니다.")
    private String reasonDetail;
}
