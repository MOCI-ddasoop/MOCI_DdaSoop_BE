package com.back.domain.feed.entity;

/**
 * 피드 타입 구분
 * GENERAL: 일반 피드 (메인 페이지에서 작성한 일반 게시물)
 * TOGETHER_VERIFICATION: 함께하기 인증 피드 (함께하기 활동 인증 게시물)
 * TOGETHER_NOTICE: 함께하기 공지 피드 (방장만 작성 가능, 상단 고정 가능)
 */
public enum FeedType {
    GENERAL,
    TOGETHER_VERIFICATION,
    TOGETHER_NOTICE
}
