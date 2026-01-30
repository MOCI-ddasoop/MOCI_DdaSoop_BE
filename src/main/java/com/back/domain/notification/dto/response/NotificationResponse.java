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
 * 알림 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    // ========== 알림 기본 정보 ==========
    
    private Long id;
    private NotificationType notificationType;
    private String message;
    private Boolean isRead;
    private LocalDateTime readAt;
    
    // ========== 발신자 정보 (시스템 알림은 null) ==========
    
    private Long senderId;
    private String senderNickname;
    private String senderProfileImage;
    
    // ========== 알림 대상 정보 (클릭 시 이동) ==========
    
    private NotificationTargetType targetType;
    private Long targetId;
    
    // ========== 시간 정보 ==========
    
    private LocalDateTime createdAt;
    
    // ========== 정적 팩토리 메서드 ==========

    /**
     * Notification 엔티티 → NotificationResponse 변환
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .senderId(notification.getSender() != null ? 
                        notification.getSender().getId() : null)
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
