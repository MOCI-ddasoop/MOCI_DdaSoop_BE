package com.back.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시스템 알림 생성 요청 DTO (관리자용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemNotificationCreateRequest {

    /**
     * 알림 메시지
     */
    @NotBlank(message = "알림 메시지는 필수입니다.")
    @Size(max = 500, message = "알림 메시지는 최대 500자까지 입력 가능합니다.")
    private String message;
}
