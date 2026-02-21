package com.back.domain.feed.entity;

import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 회원별 자주 사용하는 태그 통계 (캐싱용)
 * 
 * 목적:
 * - 피드 추천 알고리즘에서 사용
 * - 회원이 좋아요 누른 피드의 태그 분석 결과를 미리 저장
 * - 매번 JOIN + GROUP BY 하지 않고 빠르게 조회
 * 
 * 업데이트 시점:
 * - 회원이 피드에 좋아요를 누를 때 (비동기)
 * - 회원이 피드 좋아요를 취소할 때 (비동기)
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "member_tag_statistics",
    indexes = {
        @Index(name = "idx_member_tag_stats_member", columnList = "member_id"),
        @Index(name = "idx_member_tag_stats_updated", columnList = "updated_at")
    }
)
public class MemberTagStatistics extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    /**
     * 자주 사용하는 태그 목록 (상위 10개)
     * 사용 빈도 높은 순으로 정렬됨
     */
    @ElementCollection
    @CollectionTable(
        name = "member_frequent_tags",
        joinColumns = @JoinColumn(name = "member_tag_statistics_id")
    )
    @Column(name = "tag_name", length = 50)
    @OrderColumn(name = "tag_order")
    private List<String> frequentTags = new ArrayList<>();

    /**
     * 통계가 마지막으로 업데이트된 시간
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 통계 계산에 사용된 좋아요 개수
     * - 좋아요 개수 변화가 작으면 재계산 스킵
     */
    @Column(nullable = false)
    private Integer reactionCount = 0;

    // ========== 비즈니스 로직 ==========

    /**
     * 통계 업데이트
     * 
     * @param newFrequentTags 새로운 자주 사용하는 태그 목록
     * @param newReactionCount 새로운 좋아요 개수
     */
    public void updateStatistics(List<String> newFrequentTags, Integer newReactionCount) {
        this.frequentTags.clear();
        this.frequentTags.addAll(newFrequentTags);
        this.reactionCount = newReactionCount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 통계가 최신인지 확인
     * 
     * @param currentReactionCount 현재 좋아요 개수
     * @return 재계산이 필요하면 false
     */
    public boolean isUpToDate(Integer currentReactionCount) {
        // 좋아요 개수 차이가 5개 미만이면 최신으로 간주
        return Math.abs(this.reactionCount - currentReactionCount) < 5;
    }

    /**
     * 통계가 오래되었는지 확인 (7일 이상)
     * 
     * @return 7일 이상 지났으면 true
     */
    public boolean isStale() {
        return this.updatedAt.isBefore(LocalDateTime.now().minusDays(7));
    }

    /**
     * 통계 초기화
     */
    public void clear() {
        this.frequentTags.clear();
        this.reactionCount = 0;
        this.updatedAt = LocalDateTime.now();
    }
}
