package com.back.domain.notification.controller;

import com.back.domain.notification.dto.request.SystemNotificationCreateRequest;
import com.back.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 알림 컨트롤러
 * 시스템 알림 전송
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    /**
     * 특정 회원에게 시스템 알림 전송
     * 
     * POST /api/admin/notifications/system/{memberId}
     * 
     * @param memberId 수신자 회원 ID
     * @param request 시스템 알림 생성 요청
     * @return 생성된 알림 ID
     */
    @PostMapping("/system/{memberId}")
    public ResponseEntity<Long> sendSystemNotification(
            @PathVariable Long memberId,
            @Valid @RequestBody SystemNotificationCreateRequest request
    ) {
        Long notificationId = notificationService.createSystemNotification(
                memberId,
                request.getMessage()
        );

        log.info("시스템 알림 전송 API 호출 - 수신자: {}, 메시지: {}", memberId, request.getMessage());

        return ResponseEntity.ok(notificationId);
    }

    /**
     * 전체 회원에게 시스템 알림 전송 (공지사항)
     * 
     * POST /api/admin/notifications/system/broadcast
     * 
     * @param request 시스템 알림 생성 요청
     * @return 전송된 알림 개수
     */
    @PostMapping("/system/broadcast")
    public ResponseEntity<Integer> broadcastSystemNotification(
            @Valid @RequestBody SystemNotificationCreateRequest request
    ) {
        int sentCount = notificationService.broadcastSystemNotification(request.getMessage());

        log.info("전체 시스템 알림 전송 API 호출 - 수신자: {}명, 메시지: {}", 
                sentCount, request.getMessage());

        return ResponseEntity.ok(sentCount);
    }
}
