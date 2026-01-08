package com.back.domain.notification.entity;

/**
 * 알림 대상 타입
 * 알림을 클릭했을 때 어디로 이동할지 결정
 */
public enum NotificationTargetType {
    FEED,       // 피드로 이동
    COMMENT,    // 댓글로 이동
    TOGETHER,   // 함께하기 모임으로 이동
    MEMBER,     // 회원 프로필로 이동
    NONE        // 이동 없음 (시스템 알림 등)
}
