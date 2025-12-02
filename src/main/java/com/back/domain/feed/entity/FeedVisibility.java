package com.back.domain.feed.entity;

/**
 * 피드 공개 범위
 * PUBLIC: 전체 공개
 * FOLLOWERS: 팔로워만 공개
 * PRIVATE: 비공개
 */
public enum FeedVisibility {
    PUBLIC,
    FOLLOWERS,
    PRIVATE
}
