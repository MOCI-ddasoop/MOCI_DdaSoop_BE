package com.back.domain.feed.repository;

import com.back.domain.feed.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * FeedImage Repository
 */
public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    /**
     * 특정 피드의 모든 이미지 조회 (순서대로)
     * 사용 예:
     * - 피드 상세 조회 시 이미지 목록 표시
     * - 이미지 캐러셀/슬라이더
     */
    List<FeedImage> findByFeedIdOrderByDisplayOrderAsc(Long feedId);

    /**
     * 특정 피드의 첫 번째 이미지 조회 (썸네일용)
     * - 피드 목록에서 썸네일 표시
     */
    @Query("SELECT fi FROM FeedImage fi " +
           "WHERE fi.feed.id = :feedId " +
           "ORDER BY fi.displayOrder ASC " +
           "LIMIT 1")
    FeedImage findFirstByFeedIdOrderByDisplayOrderAsc(@Param("feedId") Long feedId);

    /**
     * 특정 피드의 이미지 개수
     */
    Long countByFeedId(Long feedId);

    /**
     * 특정 피드의 모든 이미지 삭제
     * - 피드 삭제 시 (cascade로 자동 삭제되지만 명시적 삭제 필요 시)
     * - 피드 수정 시 기존 이미지 전체 삭제 후 새로 추가
     */
    @Modifying
    @Query("DELETE FROM FeedImage fi WHERE fi.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 URL의 이미지 존재 여부 확인
     * - 중복 이미지 업로드 방지
     * - 이미지 URL 검증
     */
    boolean existsByImageUrl(String imageUrl);
}
