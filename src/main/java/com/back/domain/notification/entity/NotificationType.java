package com.back.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 타입
 * 어떤 종류의 알림인지 구분
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    
    // ========== Feed 관련 알림 ==========
    FEED_REACTION("님이 회원님의 피드에 좋아요를 눌렀습니다."),
    FEED_COMMENT("님이 회원님의 피드에 댓글을 남겼습니다."),
    FEED_COMMENT_REPLY("님이 회원님의 댓글에 답글을 남겼습니다."),
    
    // ========== Comment 관련 알림 ==========
    COMMENT_REACTION("님이 회원님의 댓글에 좋아요를 눌렀습니다."),
    
    // ========== Together 관련 알림 ==========
    TOGETHER_INVITE("님이 회원님을 함께하기 모임에 초대했습니다."),
    TOGETHER_JOIN("님이 회원님의 함께하기 모임에 참여했습니다."),
    TOGETHER_START("함께하기 모임이 시작되었습니다."),
    TOGETHER_END("함께하기 모임이 종료되었습니다."),
    TOGETHER_CREATE("함께하기 모임이 생성되었습니다."),
    TOGETHER_PARTICIPATE("함께하기 모임 참여가 완료되었습니다."),
    TOGETHER_LEAVE("함께하기 모임에서 탈퇴했습니다."),
    TOGETHER_LEAVE_MEMBER("님이 함께하기 모임에서 탈퇴했습니다."),
    TOGETHER_DROP("함께하기 모임에서 강퇴되었습니다."),

    // ========== Donation 관련 알림 ==========
    DONATION_RECEIVED("님이 후원해주셨습니다."),
    DONATION_COMPLETE("후원이 완료되었습니다."),
    DONATION_NOTICE("후원 공지가 등록되었습니다."),

    // ========== Follow 관련 알림 ==========
    FOLLOW("님이 회원님을 팔로우했습니다."),
    
    // ========== 시스템 알림 ==========
    SYSTEM("시스템 알림입니다.");

    private final String messageTemplate;
}
