package com.back.domain.feed.entity;

/**
 * 피드 공개 범위
 * PUBLIC: 전체 공개
 * FOLLOWERS: 팔로워만 공개
 * PRIVATE: 비공개
 * MEMBERS: 함께하기 모임 멤버만 공개
 * NOTICE: 공지사항 (함께하기 모임 내 공지)
 */
public enum FeedVisibility {
    PUBLIC,
    FOLLOWERS,
    PRIVATE,
    MEMBERS,
    NOTICE
}
