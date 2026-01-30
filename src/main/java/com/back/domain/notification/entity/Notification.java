package com.back.domain.notification.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 알림 엔티티
 * 사용자에게 전달할 알림 정보를 저장
 */
@Entity
@Table(name = "notifications")
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    // ========== 알림 수신자 ==========
    
    /** 알림을 받는 사람 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    // ========== 알림 발신자 (선택) ==========
    
    /** 
     * 알림을 발생시킨 사람 (누가 좋아요/댓글을 남겼는지)
     * 시스템 알림인 경우 null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    // ========== 알림 타입 ==========
    
    /** 알림 타입 (FEED_REACTION, COMMENT, FOLLOW 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    // ========== 알림 대상 (다형성) ==========
    
    /** 
     * 알림 대상 타입 (클릭 시 이동할 곳)
     * FEED, COMMENT, TOGETHER, MEMBER, NONE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private NotificationTargetType targetType;

    /** 
     * 알림 대상 ID
     * targetType이 FEED → Feed ID
     * targetType이 COMMENT → Comment ID
     * targetType이 TOGETHER → Together ID
     * targetType이 MEMBER → Member ID
     */
    @Column(name = "target_id")
    private Long targetId;

    // ========== 알림 내용 ==========
    
    /** 
     * 알림 메시지 (최대 500자)
     * 예: "홍길동님이 회원님의 피드에 좋아요를 눌렀습니다."
     */
    @Column(nullable = false, length = 500)
    private String message;

    // ========== 읽음 상태 ==========
    
    /** 읽음 여부 (기본값: false) */
    @Column(nullable = false)
    @lombok.Builder.Default
    private Boolean isRead = false;

    /** 읽은 시간 */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ========== Soft Delete ==========
    
    /** 삭제 시간 (사용자가 알림 삭제 시) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========== 비즈니스 로직 ==========

    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * 알림 삭제 (Soft Delete)
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 알림 복구
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * 삭제되었는지 확인
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 읽지 않은 알림인지 확인
     */
    public boolean isUnread() {
        return !this.isRead;
    }

    // ========== 헬퍼 메서드 ==========

    /**
     * 알림 메시지 생성
     * sender의 닉네임 + 알림 타입의 메시지 템플릿 조합
     * 
     * @param senderNickname 발신자 닉네임
     * @return 완성된 알림 메시지
     */
    public static String createMessage(String senderNickname, NotificationType type) {
        if (senderNickname == null || senderNickname.isBlank()) {
            return type.getMessageTemplate();
        }
        return senderNickname + type.getMessageTemplate();
    }

    /**
     * 시스템 알림 여부 확인
     */
    public boolean isSystemNotification() {
        return this.notificationType == NotificationType.SYSTEM;
    }

    /**
     * Feed 관련 알림인지 확인
     */
    public boolean isFeedNotification() {
        return this.notificationType == NotificationType.FEED_REACTION
                || this.notificationType == NotificationType.FEED_COMMENT
                || this.notificationType == NotificationType.FEED_COMMENT_REPLY;
    }

    /**
     * Comment 관련 알림인지 확인
     */
    public boolean isCommentNotification() {
        return this.notificationType == NotificationType.COMMENT_REACTION;
    }

    /**
     * Together 관련 알림인지 확인
     */
    public boolean isTogetherNotification() {
        return this.notificationType == NotificationType.TOGETHER_INVITE
                || this.notificationType == NotificationType.TOGETHER_JOIN
                || this.notificationType == NotificationType.TOGETHER_START
                || this.notificationType == NotificationType.TOGETHER_END;
    }

    /**
     * Follow 관련 알림인지 확인
     */
    public boolean isFollowNotification() {
        return this.notificationType == NotificationType.FOLLOW;
    }
}
