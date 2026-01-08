package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification Repository
 * 알림 데이터 조회, 관리
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ========== 알림 목록 조회 ==========

    /**
     * 특정 회원의 알림 목록 조회 (삭제되지 않은 것만, 최신순, 페이징)
     *
     * @param receiverId 수신자 ID
     * @param pageable 페이징 정보
     * @return 알림 목록
     */
    Page<Notification> findByReceiver_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long receiverId, 
            Pageable pageable
    );

    /**
     * 특정 회원의 읽지 않은 알림 목록 조회 (최신순, 페이징)
     *
     * @param receiverId 수신자 ID
     * @param pageable 페이징 정보
     * @return 읽지 않은 알림 목록
     */
    Page<Notification> findByReceiver_IdAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long receiverId, 
            Pageable pageable
    );

    /**
     * 특정 회원의 특정 타입 알림 조회 (최신순)
     *
     * @param receiverId 수신자 ID
     * @param notificationType 알림 타입
     * @param pageable 페이징 정보
     * @return 특정 타입의 알림 목록
     */
    Page<Notification> findByReceiver_IdAndNotificationTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long receiverId,
            NotificationType notificationType,
            Pageable pageable
    );

    // ========== 알림 개수 조회 ==========

    /**
     * 특정 회원의 읽지 않은 알림 개수
     *
     * @param receiverId 수신자 ID
     * @return 읽지 않은 알림 개수
     */
    Long countByReceiver_IdAndIsReadFalseAndDeletedAtIsNull(Long receiverId);

    /**
     * 특정 회원의 전체 알림 개수 (삭제되지 않은 것만)
     *
     * @param receiverId 수신자 ID
     * @return 전체 알림 개수
     */
    Long countByReceiver_IdAndDeletedAtIsNull(Long receiverId);

    // ========== 일괄 읽음 처리 ==========

    /**
     * 특정 회원의 모든 알림을 읽음 처리
     * 
     * @param receiverId 수신자 ID
     * @param readAt 읽은 시간
     * @return 업데이트된 알림 개수
     */
    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.receiver.id = :receiverId " +
           "AND n.isRead = false " +
           "AND n.deletedAt IS NULL")
    int markAllAsReadByReceiverId(
            @Param("receiverId") Long receiverId, 
            @Param("readAt") LocalDateTime readAt
    );

    /**
     * 특정 ID 목록의 알림을 읽음 처리
     * 
     * @param notificationIds 알림 ID 목록
     * @param readAt 읽은 시간
     * @return 업데이트된 알림 개수
     */
    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.id IN :notificationIds " +
           "AND n.isRead = false " +
           "AND n.deletedAt IS NULL")
    int markAsReadByIds(
            @Param("notificationIds") List<Long> notificationIds,
            @Param("readAt") LocalDateTime readAt
    );

    // ========== 일괄 삭제 ==========

    /**
     * 특정 회원의 읽은 알림 모두 삭제 (Soft Delete)
     * 
     * @param receiverId 수신자 ID
     * @param deletedAt 삭제 시간
     * @return 삭제된 알림 개수
     */
    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.deletedAt = :deletedAt " +
           "WHERE n.receiver.id = :receiverId " +
           "AND n.isRead = true " +
           "AND n.deletedAt IS NULL")
    int deleteAllReadByReceiverId(
            @Param("receiverId") Long receiverId,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    /**
     * 특정 회원의 모든 알림 삭제 (Soft Delete)
     * 
     * @param receiverId 수신자 ID
     * @param deletedAt 삭제 시간
     * @return 삭제된 알림 개수
     */
    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.deletedAt = :deletedAt " +
           "WHERE n.receiver.id = :receiverId " +
           "AND n.deletedAt IS NULL")
    int deleteAllByReceiverId(
            @Param("receiverId") Long receiverId,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    /**
     * 특정 ID 목록의 알림 삭제 (Soft Delete)
     * 
     * @param notificationIds 알림 ID 목록
     * @param deletedAt 삭제 시간
     * @return 삭제된 알림 개수
     */
    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.deletedAt = :deletedAt " +
           "WHERE n.id IN :notificationIds " +
           "AND n.deletedAt IS NULL")
    int deleteByIds(
            @Param("notificationIds") List<Long> notificationIds,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    // ========== 중복 알림 방지 ==========

    /**
     * 동일한 알림이 최근에 생성되었는지 확인 (중복 방지)
     * 예: 알림이 5분 내에 이미 있으면 중복 생성 방지
     * 
     * @param receiverId 수신자 ID
     * @param senderId 발신자 ID
     * @param notificationType 알림 타입
     * @param targetId 대상 ID
     * @param since 이 시간 이후 생성된 알림만 확인
     * @return 중복 알림 존재 여부
     */
    boolean existsByReceiver_IdAndSender_IdAndNotificationTypeAndTargetIdAndCreatedAtAfter(
            Long receiverId,
            Long senderId,
            NotificationType notificationType,
            Long targetId,
            LocalDateTime since
    );

    // ========== 오래된 알림 정리 (관리자용) ==========

    /**
     * 특정 날짜 이전에 생성된 읽은 알림 삭제
     * 예: 30일 이상 된 읽은 알림 자동 삭제
     * 
     * @param before 이 날짜 이전의 알림
     * @param deletedAt 삭제 시간
     * @return 삭제된 알림 개수
     */
    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.deletedAt = :deletedAt " +
           "WHERE n.isRead = true " +
           "AND n.createdAt < :before " +
           "AND n.deletedAt IS NULL")
    int deleteOldReadNotifications(
            @Param("before") LocalDateTime before,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    // ========== 최근 알림 조회 (빠른 조회) ==========

    /**
     * 특정 회원의 최근 N개 알림 조회 (읽지 않은 것 우선)
     * 
     * @param receiverId 수신자 ID
     * @param pageable 페이징 정보 (size로 개수 제한)
     * @return 최근 알림 목록
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.receiver.id = :receiverId " +
           "AND n.deletedAt IS NULL " +
           "ORDER BY n.isRead ASC, n.createdAt DESC")
    List<Notification> findRecentNotifications(
            @Param("receiverId") Long receiverId,
            Pageable pageable
    );
}
