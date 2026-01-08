package com.back.domain.notification.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationTargetType;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification Service
 * 알림 생성, 조회, 읽음 처리, 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    // ========== 알림 생성 ==========

    /**
     * 알림 생성 (중복 방지 포함)
     * 
     * @param receiverId 수신자 ID
     * @param senderId 발신자 ID (시스템 알림인 경우 null)
     * @param notificationType 알림 타입
     * @param targetType 대상 타입
     * @param targetId 대상 ID
     * @return 생성된 알림 ID (중복인 경우 null)
     */
    @Transactional
    public Long createNotification(
            Long receiverId,
            Long senderId,
            NotificationType notificationType,
            NotificationTargetType targetType,
            Long targetId
    ) {
        // 1. 수신자 조회
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        // 2. 자기 자신에게 알림 보내지 않기
        if (senderId != null && senderId.equals(receiverId)) {
            log.debug("자기 자신에게 알림 전송 차단 - receiverId: {}", receiverId);
            return null;
        }

        // 3. 발신자 조회 (시스템 알림이 아닌 경우)
        Member sender = null;
        if (senderId != null) {
            sender = memberRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));
        }

        // 4. 중복 알림 확인 (5분 이내)
        if (senderId != null && isDuplicateNotification(receiverId, senderId, notificationType, targetId)) {
            log.debug("중복 알림 차단 - receiver: {}, sender: {}, type: {}, target: {}", 
                    receiverId, senderId, notificationType, targetId);
            return null;
        }

        // 5. 알림 메시지 생성
        String message = createMessage(sender, notificationType);

        // 6. 알림 생성
        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .notificationType(notificationType)
                .targetType(targetType)
                .targetId(targetId)
                .message(message)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        log.info("알림 생성 완료 - ID: {}, receiver: {}, type: {}", 
                savedNotification.getId(), receiverId, notificationType);

        return savedNotification.getId();
    }

    /**
     * 시스템 알림 생성 (발신자 없음)
     * 
     * @param receiverId 수신자 ID
     * @param message 알림 메시지
     * @return 생성된 알림 ID
     */
    @Transactional
    public Long createSystemNotification(Long receiverId, String message) {
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(null)
                .notificationType(NotificationType.SYSTEM)
                .targetType(NotificationTargetType.NONE)
                .targetId(null)
                .message(message)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        log.info("시스템 알림 생성 완료 - ID: {}, receiver: {}", 
                savedNotification.getId(), receiverId);

        return savedNotification.getId();
    }

    /**
     * 전체 회원에게 시스템 알림 전송 (공지사항 등)
     * 
     * @param message 알림 메시지
     * @return 전송된 알림 개수
     */
    @Transactional
    public int broadcastSystemNotification(String message) {
        List<Member> allMembers = memberRepository.findAll();

        int count = 0;
        for (Member member : allMembers) {
            Notification notification = Notification.builder()
                    .receiver(member)
                    .sender(null)
                    .notificationType(NotificationType.SYSTEM)
                    .targetType(NotificationTargetType.NONE)
                    .targetId(null)
                    .message(message)
                    .build();

            notificationRepository.save(notification);
            count++;
        }

        log.info("전체 시스템 알림 전송 완료 - 수신자: {}명", count);

        return count;
    }

    // ========== 알림 조회 ==========

    /**
     * 알림 상세 조회
     * 
     * @param notificationId 알림 ID
     * @return 알림 정보
     */
    public Notification getNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
    }

    /**
     * 내 알림 목록 조회 (전체, 최신순, 페이징)
     * 
     * @param receiverId 수신자 ID
     * @param pageable 페이징 정보
     * @return 알림 목록
     */
    public Page<Notification> getMyNotifications(Long receiverId, Pageable pageable) {
        return notificationRepository
                .findByReceiver_IdAndDeletedAtIsNullOrderByCreatedAtDesc(receiverId, pageable);
    }

    /**
     * 내 읽지 않은 알림 목록 조회 (페이징)
     * 
     * @param receiverId 수신자 ID
     * @param pageable 페이징 정보
     * @return 읽지 않은 알림 목록
     */
    public Page<Notification> getMyUnreadNotifications(Long receiverId, Pageable pageable) {
        return notificationRepository
                .findByReceiver_IdAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDesc(receiverId, pageable);
    }

    /**
     * 특정 타입의 알림 목록 조회
     * 
     * @param receiverId 수신자 ID
     * @param notificationType 알림 타입
     * @param pageable 페이징 정보
     * @return 특정 타입의 알림 목록
     */
    public Page<Notification> getNotificationsByType(
            Long receiverId, 
            NotificationType notificationType, 
            Pageable pageable
    ) {
        return notificationRepository
                .findByReceiver_IdAndNotificationTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        receiverId, notificationType, pageable
                );
    }

    /**
     * 최근 알림 빠르게 조회 (읽지 않은 것 우선, 드롭다운용)
     * 
     * @param receiverId 수신자 ID
     * @param limit 조회 개수
     * @return 최근 알림 목록
     */
    public List<Notification> getRecentNotifications(Long receiverId, int limit) {
        return notificationRepository.findRecentNotifications(
                receiverId, 
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    // ========== 알림 개수 조회 ==========

    /**
     * 읽지 않은 알림 개수 (뱃지 표시용)
     * 
     * @param receiverId 수신자 ID
     * @return 읽지 않은 알림 개수
     */
    public Long getUnreadNotificationCount(Long receiverId) {
        return notificationRepository.countByReceiver_IdAndIsReadFalseAndDeletedAtIsNull(receiverId);
    }

    /**
     * 전체 알림 개수
     * 
     * @param receiverId 수신자 ID
     * @return 전체 알림 개수
     */
    public Long getTotalNotificationCount(Long receiverId) {
        return notificationRepository.countByReceiver_IdAndDeletedAtIsNull(receiverId);
    }

    // ========== 알림 읽음 처리 ==========

    /**
     * 단일 알림 읽음 처리
     * 
     * @param notificationId 알림 ID
     * @param currentMemberId 현재 사용자 ID
     */
    @Transactional
    public void markAsRead(Long notificationId, Long currentMemberId) {
        Notification notification = getNotification(notificationId);

        // 권한 확인 (본인의 알림만 읽음 처리 가능)
        if (!notification.getReceiver().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException("알림 읽음 처리 권한이 없습니다.");
        }

        notification.markAsRead();

        log.info("알림 읽음 처리 - ID: {}", notificationId);
    }

    /**
     * 여러 알림 읽음 처리
     * 
     * @param notificationIds 알림 ID 목록
     * @param currentMemberId 현재 사용자 ID
     * @return 읽음 처리된 알림 개수
     */
    @Transactional
    public int markAsReadBatch(List<Long> notificationIds, Long currentMemberId) {
        // TODO: 권한 확인 (본인의 알림만 처리 가능하도록)
        // 현재는 일괄 처리 성능 우선

        int updatedCount = notificationRepository.markAsReadByIds(
                notificationIds, 
                LocalDateTime.now()
        );

        log.info("알림 일괄 읽음 처리 - {}개", updatedCount);

        return updatedCount;
    }

    /**
     * 모든 알림 읽음 처리
     * 
     * @param receiverId 수신자 ID
     * @return 읽음 처리된 알림 개수
     */
    @Transactional
    public int markAllAsRead(Long receiverId) {
        int updatedCount = notificationRepository.markAllAsReadByReceiverId(
                receiverId, 
                LocalDateTime.now()
        );

        log.info("전체 알림 읽음 처리 - 수신자: {}, {}개", receiverId, updatedCount);

        return updatedCount;
    }

    // ========== 알림 삭제 ==========

    /**
     * 단일 알림 삭제 (Soft Delete)
     * 
     * @param notificationId 알림 ID
     * @param currentMemberId 현재 사용자 ID
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long currentMemberId) {
        Notification notification = getNotification(notificationId);

        // 권한 확인
        if (!notification.getReceiver().getId().equals(currentMemberId)) {
            throw new IllegalArgumentException("알림 삭제 권한이 없습니다.");
        }

        notification.delete();

        log.info("알림 삭제 - ID: {}", notificationId);
    }

    /**
     * 여러 알림 삭제 (Soft Delete)
     * 
     * @param notificationIds 알림 ID 목록
     * @param currentMemberId 현재 사용자 ID
     * @return 삭제된 알림 개수
     */
    @Transactional
    public int deleteNotificationsBatch(List<Long> notificationIds, Long currentMemberId) {
        // TODO: 권한 확인 (본인의 알림만 삭제 가능하도록)

        int deletedCount = notificationRepository.deleteByIds(
                notificationIds, 
                LocalDateTime.now()
        );

        log.info("알림 일괄 삭제 - {}개", deletedCount);

        return deletedCount;
    }

    /**
     * 읽은 알림 모두 삭제
     * 
     * @param receiverId 수신자 ID
     * @return 삭제된 알림 개수
     */
    @Transactional
    public int deleteAllReadNotifications(Long receiverId) {
        int deletedCount = notificationRepository.deleteAllReadByReceiverId(
                receiverId, 
                LocalDateTime.now()
        );

        log.info("읽은 알림 전체 삭제 - 수신자: {}, {}개", receiverId, deletedCount);

        return deletedCount;
    }

    /**
     * 모든 알림 삭제
     * 
     * @param receiverId 수신자 ID
     * @return 삭제된 알림 개수
     */
    @Transactional
    public int deleteAllNotifications(Long receiverId) {
        int deletedCount = notificationRepository.deleteAllByReceiverId(
                receiverId, 
                LocalDateTime.now()
        );

        log.info("전체 알림 삭제 - 수신자: {}, {}개", receiverId, deletedCount);

        return deletedCount;
    }

    // ========== Private 헬퍼 메서드 ==========

    /**
     * 중복 알림 확인 (5분 이내)
     * 
     * @param receiverId 수신자 ID
     * @param senderId 발신자 ID
     * @param notificationType 알림 타입
     * @param targetId 대상 ID
     * @return 중복 여부
     */
    private boolean isDuplicateNotification(
            Long receiverId,
            Long senderId,
            NotificationType notificationType,
            Long targetId
    ) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        return notificationRepository.existsByReceiver_IdAndSender_IdAndNotificationTypeAndTargetIdAndCreatedAtAfter(
                receiverId, senderId, notificationType, targetId, fiveMinutesAgo
        );
    }

    /**
     * 알림 메시지 생성
     * 
     * @param sender 발신자 (시스템 알림인 경우 null)
     * @param notificationType 알림 타입
     * @return 알림 메시지
     */
    private String createMessage(Member sender, NotificationType notificationType) {
        if (sender == null) {
            return notificationType.getMessageTemplate();
        }

        return sender.getNickname() + notificationType.getMessageTemplate();
    }
}
