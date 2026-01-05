package com.back.domain.notification.dto.response;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationTargetType;
import com.back.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 요약 응답 DTO (목록 조회용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryResponse {

    private Long id;
    private NotificationType notificationType;
    private String message;
    private Boolean isRead;
    private String senderNickname;
    private String senderProfileImage;
    private NotificationTargetType targetType;
    private Long targetId;
    private LocalDateTime createdAt;

    public static NotificationSummaryResponse from(Notification notification) {
        return NotificationSummaryResponse.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .senderNickname(notification.getSender() != null ? 
                        notification.getSender().getNickname() : null)
                .senderProfileImage(notification.getSender() != null ? 
                        notification.getSender().getProfileImageUrl() : null)
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
