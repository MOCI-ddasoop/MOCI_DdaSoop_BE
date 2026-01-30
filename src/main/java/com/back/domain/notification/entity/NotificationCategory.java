package com.back.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 알림 카테고리
 * 프론트엔드 탭별로 알림을 그룹핑하기 위한 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum NotificationCategory {
    
    /**
     * 좋아요 탭
     * - 피드 좋아요
     * - 댓글 좋아요
     */
    LIKES("좋아요", List.of(
            NotificationType.FEED_REACTION,
            NotificationType.COMMENT_REACTION
    )),
    
    /**
     * 댓글 탭
     * - 피드 댓글
     * - 댓글 답글
     */
    COMMENTS("댓글", List.of(
            NotificationType.FEED_COMMENT,
            NotificationType.FEED_COMMENT_REPLY
    )),
    
    /**
     * 함께하기 탭
     * - 모임 초대
     * - 모임 참여
     * - 모임 시작
     * - 모임 종료
     */
    TOGETHER("함께하기", List.of(
            NotificationType.TOGETHER_INVITE,
            NotificationType.TOGETHER_JOIN,
            NotificationType.TOGETHER_START,
            NotificationType.TOGETHER_END
    )),
    
    /**
     * 시스템 탭
     * - 시스템 알림
     */
    SYSTEM("시스템", List.of(
            NotificationType.SYSTEM
    )),
    
    /**
     * 팔로우 탭 (추후 구현)
     * - 팔로우 알림
     */
    FOLLOW("팔로우", List.of(
            NotificationType.FOLLOW
    ));

    private final String displayName;
    private final List<NotificationType> notificationTypes;

    /**
     * 카테고리에 속한 NotificationType 목록 반환
     * 
     * @return NotificationType 리스트
     */
    public List<NotificationType> getTypes() {
        return notificationTypes;
    }

    /**
     * 특정 NotificationType이 어떤 카테고리에 속하는지 찾기
     * 
     * @param notificationType 알림 타입
     * @return 해당하는 카테고리 (없으면 null)
     */
    public static NotificationCategory fromNotificationType(NotificationType notificationType) {
        return Arrays.stream(values())
                .filter(category -> category.getTypes().contains(notificationType))
                .findFirst()
                .orElse(null);
    }
}
