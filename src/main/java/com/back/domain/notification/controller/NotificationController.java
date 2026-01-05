package com.back.domain.notification.controller;

import com.back.domain.notification.dto.response.NotificationResponse;
import com.back.domain.notification.dto.response.NotificationSummaryResponse;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 컨트롤러 (사용자용)
 * 알림 조회, 읽음 처리, 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ========== 알림 조회 ==========

    /**
     * 내 알림 목록 조회 (전체, 페이징)
     * 
     * GET /api/notifications?page=0&size=20
     * 
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 목록
     */
    @GetMapping
    public ResponseEntity<Page<NotificationSummaryResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
            // TODO: @AuthenticationPrincipal 또는 @CurrentMember로 현재 사용자 ID 가져오기
    ) {
        // 임시: 현재 사용자 ID
        Long currentMemberId = 1L;

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getMyNotifications(currentMemberId, pageable);

        Page<NotificationSummaryResponse> response = notifications.map(NotificationSummaryResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * 내 읽지 않은 알림 목록 조회 (페이징)
     * 
     * GET /api/notifications/unread?page=0&size=20
     * 
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 읽지 않은 알림 목록
     */
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationSummaryResponse>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentMemberId = 1L;

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getMyUnreadNotifications(currentMemberId, pageable);

        Page<NotificationSummaryResponse> response = notifications.map(NotificationSummaryResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 타입의 알림 목록 조회
     * 
     * GET /api/notifications/type/FEED_REACTION?page=0&size=20
     * 
     * @param notificationType 알림 타입
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 특정 타입의 알림 목록
     */
    @GetMapping("/type/{notificationType}")
    public ResponseEntity<Page<NotificationSummaryResponse>> getNotificationsByType(
            @PathVariable NotificationType notificationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentMemberId = 1L;

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getNotificationsByType(
                currentMemberId, notificationType, pageable
        );

        Page<NotificationSummaryResponse> response = notifications.map(NotificationSummaryResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * 최근 알림 빠르게 조회 (드롭다운용)
     * 
     * GET /api/notifications/recent?limit=10
     * 
     * @param limit 조회 개수 (기본: 10)
     * @return 최근 알림 목록 (읽지 않은 것 우선)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationSummaryResponse>> getRecentNotifications(
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long currentMemberId = 1L;

        List<Notification> notifications = notificationService.getRecentNotifications(currentMemberId, limit);

        List<NotificationSummaryResponse> response = notifications.stream()
                .map(NotificationSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 알림 상세 조회
     * 
     * GET /api/notifications/{notificationId}
     * 
     * @param notificationId 알림 ID
     * @return 알림 상세 정보
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable Long notificationId
    ) {
        Long currentMemberId = 1L;

        Notification notification = notificationService.getNotification(notificationId);

        // 본인의 알림만 조회 가능
        if (!notification.getReceiver().getId().equals(currentMemberId)) {
            return ResponseEntity.status(403).build();
        }

        NotificationResponse response = NotificationResponse.from(notification);

        return ResponseEntity.ok(response);
    }

    /**
     * 읽지 않은 알림 개수 조회 (뱃지 표시용)
     * 
     * GET /api/notifications/unread-count
     * 
     * @return 읽지 않은 알림 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadNotificationCount() {
        Long currentMemberId = 1L;

        Long unreadCount = notificationService.getUnreadNotificationCount(currentMemberId);

        return ResponseEntity.ok(unreadCount);
    }

    // ========== 알림 읽음 처리 ==========

    /**
     * 단일 알림 읽음 처리
     * 
     * PUT /api/notifications/{notificationId}/read
     * 
     * @param notificationId 알림 ID
     * @return 성공 메시지
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        Long currentMemberId = 1L;

        notificationService.markAsRead(notificationId, currentMemberId);

        log.info("알림 읽음 처리 API 호출 - 알림 ID: {}", notificationId);

        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 읽음 처리
     * 
     * PUT /api/notifications/read-all
     * 
     * @return 읽음 처리된 알림 개수
     */
    @PutMapping("/read-all")
    public ResponseEntity<Integer> markAllAsRead() {
        Long currentMemberId = 1L;

        int updatedCount = notificationService.markAllAsRead(currentMemberId);

        log.info("전체 알림 읽음 처리 API 호출 - {}개", updatedCount);

        return ResponseEntity.ok(updatedCount);
    }

    // ========== 알림 삭제 ==========

    /**
     * 단일 알림 삭제
     * 
     * DELETE /api/notifications/{notificationId}
     * 
     * @param notificationId 알림 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        Long currentMemberId = 1L;

        notificationService.deleteNotification(notificationId, currentMemberId);

        log.info("알림 삭제 API 호출 - 알림 ID: {}", notificationId);

        return ResponseEntity.ok().build();
    }

    /**
     * 읽은 알림 모두 삭제
     * 
     * DELETE /api/notifications/read
     * 
     * @return 삭제된 알림 개수
     */
    @DeleteMapping("/read")
    public ResponseEntity<Integer> deleteAllReadNotifications() {
        Long currentMemberId = 1L;

        int deletedCount = notificationService.deleteAllReadNotifications(currentMemberId);

        log.info("읽은 알림 전체 삭제 API 호출 - {}개", deletedCount);

        return ResponseEntity.ok(deletedCount);
    }

    /**
     * 모든 알림 삭제
     * 
     * DELETE /api/notifications/all
     * 
     * @return 삭제된 알림 개수
     */
    @DeleteMapping("/all")
    public ResponseEntity<Integer> deleteAllNotifications() {
        Long currentMemberId = 1L;

        int deletedCount = notificationService.deleteAllNotifications(currentMemberId);

        log.info("전체 알림 삭제 API 호출 - {}개", deletedCount);

        return ResponseEntity.ok(deletedCount);
    }
}
