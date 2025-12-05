package com.back.domain.feed.repository;

import com.back.domain.feed.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    /**
     * 특정 피드의 모든 이미지 조회 (순서대로)
     */
    List<FeedImage> findByFeedIdOrderByDisplayOrderAsc(Long feedId);

    /**
     * 특정 피드의 첫 번째 이미지 조회 (썸네일용)
     */
    @Query("SELECT fi FROM FeedImage fi WHERE fi.feed.id = :feedId ORDER BY fi.displayOrder ASC LIMIT 1")
    FeedImage findFirstByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 이미지 URL로 조회 (S3 삭제 시 사용)
     */
    List<FeedImage> findByImageUrl(String imageUrl);

    /**
     * 특정 피드의 이미지 개수
     */
    Long countByFeedId(Long feedId);

    /**
     * 특정 피드의 모든 이미지 삭제
     */
    void deleteByFeedId(Long feedId);
}
