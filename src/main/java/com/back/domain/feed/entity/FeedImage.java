package com.back.domain.feed.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 피드 이미지 엔티티
 * 이미지 URL과 메타데이터(width, height)를 저장
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "feed_image",
    indexes = {
        @Index(name = "idx_feed_image_feed_id", columnList = "feed_id")
    }
)
public class FeedImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;                     // 피드 참조

    @Column(nullable = false, length = 500)
    private String imageUrl;               // S3 이미지 URL

    @Column(nullable = false)
    private Integer width;                 // 이미지 가로 크기 (px)

    @Column(nullable = false)
    private Integer height;                // 이미지 세로 크기 (px)

    @Column(nullable = false)
    private Integer displayOrder;          // 이미지 표시 순서 (0부터 시작)

    @Column
    private Long fileSize;                 // 파일 크기 (bytes, 선택사항)

    @Column(length = 100)
    private String originalFileName;       // 원본 파일명 (선택사항)

    // ========== 비즈니스 로직 ==========

    /**
     * 이미지 가로세로 비율 계산
     */
    public double getAspectRatio() {
        if (height == 0) {
            return 0;
        }
        return (double) width / height;
    }

    /**
     * 세로형 이미지인지 확인 (세로 > 가로)
     */
    public boolean isPortrait() {
        return height > width;
    }

    /**
     * 가로형 이미지인지 확인 (가로 > 세로)
     */
    public boolean isLandscape() {
        return width > height;
    }

    /**
     * 정사각형 이미지인지 확인
     */
    public boolean isSquare() {
        return width.equals(height);
    }

    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
