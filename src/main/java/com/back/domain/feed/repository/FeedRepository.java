package com.back.domain.feed.repository;

import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.entity.FeedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    // ========== 기본 조회 ==========

    /**
     * 삭제되지 않은 피드 전체 조회 (페이징)
     * 사용 예: 메인 피드 목록, 최신 피드 목록
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 피드 페이지
     */
    Page<Feed> findByDeletedAtIsNull(Pageable pageable);

    /**
     * ID로 삭제되지 않은 피드 단건 조회
     * 사용 예: 피드 상세 페이지
     * @param id 피드 ID
     * @return Optional<Feed> (삭제되었거나 없으면 empty)
     */
    Optional<Feed> findByIdAndDeletedAtIsNull(Long id);


    // ========== 무한 스크롤 (커서 기반 페이징) ==========

    /**
     * 무한 스크롤: ID 기준 (최신순, 최대 20개)
     * 사용 예: 모바일 앱 피드 스크롤
     * @param lastFeedId 마지막으로 조회한 피드 ID (첫 조회면 Long.MAX_VALUE)
     * @return 최대 20개 피드 리스트
     * 
     * 동작 원리:
     * - 첫 조회: lastFeedId = Long.MAX_VALUE → ID가 큰 순서대로 20개
     * - 두 번째: lastFeedId = 980 → 980보다 작은 ID 중 20개 (979, 978, ...)
     */
    List<Feed> findTop20ByIdLessThanAndDeletedAtIsNullOrderByIdDesc(Long lastFeedId);

    /**
     * 무한 스크롤: 생성일 기준 (최신순, 최대 20개)
     * 사용 예: 시간 기반 정렬이 중요한 경우
     * @param lastCreatedAt 마지막으로 조회한 피드의 생성 시각
     * @return 최대 20개 피드 리스트
     */
    List<Feed> findTop20ByCreatedAtLessThanAndDeletedAtIsNullOrderByCreatedAtDesc(
            LocalDateTime lastCreatedAt
    );

    // ========== 특정 회원의 피드 ==========

    /**
     * 특정 회원이 작성한 피드 조회 (페이징)
     * 사용 예: 마이페이지, 프로필 페이지의 "내가 쓴 글"
     * @param memberId 회원 ID
     * @param pageable 페이징 정보
     * @return 해당 회원의 피드 페이지
     */
    Page<Feed> findByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);

    /**
     * 특정 회원이 작성한 피드 개수
     * 사용 예: "총 게시물 123개" 표시
     * @param memberId 회원 ID
     * @return 피드 개수
     */
    Long countByMemberIdAndDeletedAtIsNull(Long memberId);

    /**
     * 특정 회원의 피드 무한 스크롤
     * 사용 예: 특정 회원의 프로필에서 스크롤
     * @param memberId 회원 ID
     * @param lastFeedId 마지막 피드 ID
     * @return 최대 20개 피드
     */
    List<Feed> findTop20ByMemberIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            Long memberId,
            Long lastFeedId
    );

    // ========== 피드 타입별 조회 ==========

    /**
     * 피드 타입별 조회 (일반 피드 또는 함께하기 인증 피드)
     * 사용 예: "일반 피드만 보기" 또는 "인증 피드만 보기" 필터
     * @param feedType GENERAL(일반) 또는 TOGETHER_VERIFICATION(인증)
     * @param pageable 페이징 정보
     * @return 해당 타입의 피드 페이지
     */
    Page<Feed> findByFeedTypeAndDeletedAtIsNull(FeedType feedType, Pageable pageable);

    /**
     * 피드 타입별 무한 스크롤
     * @param feedType 피드 타입
     * @param lastFeedId 마지막 피드 ID
     * @return 최대 20개 피드
     */
    List<Feed> findTop20ByFeedTypeAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
            FeedType feedType,
            Long lastFeedId
    );

    // ========== 태그 검색 ==========

    /**
     * 특정 태그가 포함된 피드 검색
     * 사용 예: "#여행" 태그 클릭 시 해당 태그가 달린 모든 피드
     * @param tag 검색할 태그 (예: "여행")
     * @param pageable 페이징 정보
     * @return 해당 태그를 포함한 피드 리스트
     */
    @Query("SELECT DISTINCT f FROM Feed f JOIN f.tags t WHERE t = :tag AND f.deletedAt IS NULL")
    List<Feed> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 여러 태그 중 하나라도 포함된 피드 검색 (OR 조건)
     * 사용 예: "#여행 OR #맛집 OR #카페" 검색
     * @param tags 검색할 태그 목록 (예: ["여행", "맛집", "카페"])
     * @param pageable 페이징 정보
     * @return 해당 태그 중 하나라도 포함한 피드 리스트
     */
    @Query("SELECT DISTINCT f FROM Feed f JOIN f.tags t WHERE t IN :tags AND f.deletedAt IS NULL")
    List<Feed> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    /**
     * 모든 태그를 포함한 피드 검색 (AND 조건)
     * 사용 예: "#여행 AND #제주도" 둘 다 포함된 피드만
     * 주의: 현재는 2개 태그만 지원, 더 필요하면 동적 쿼리 사용
     */
    @Query("SELECT f FROM Feed f WHERE " +
            "f.deletedAt IS NULL AND " +
            "SIZE(f.tags) >= :tagCount AND " +
            "(:tag1 MEMBER OF f.tags) AND " +
            "(:tag2 MEMBER OF f.tags)")
    List<Feed> findByAllTags(
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tagCount") int tagCount,
            Pageable pageable
    );

    // ========== 함께하기 관련 ==========

    /**
     * 특정 Together(함께하기 모임)의 인증 피드 조회
     * 사용 예: "운동 챌린지" 모임의 인증 피드만 보기
     */
    // List<Feed> findByTogetherIdAndFeedTypeAndDeletedAtIsNull(
    //         Long togetherId,
    //         FeedType feedType,
    //         Pageable pageable
    // );

    /**
     * 특정 Together의 인증 피드 개수
     * 사용 예: "총 인증 123건" 표시
     */
    // Long countByTogetherIdAndFeedTypeAndDeletedAtIsNull(Long togetherId, FeedType feedType);

    // ========== 인기 피드 ==========

    /**
     * 리액션(좋아요)이 많은 인기 피드 조회 (최대 20개)
     * 사용 예: "인기 게시물" 탭
     * 정렬: 리액션 많은 순 → 같으면 최신순
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByReactionCountDescCreatedAtDesc();

    /**
     * 댓글이 많은 피드 조회 (최대 20개)
     * 사용 예: "토론 많은 게시물" 탭
     * 정렬: 댓글 많은 순 → 같으면 최신순
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();

    /**
     * 북마크가 많은 피드 조회 (최대 20개)
     * 사용 예: "가장 많이 저장된 게시물" 탭
     * 정렬: 북마크 많은 순 → 같으면 최신순
     */
    List<Feed> findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();

    /**
     * 특정 기간 내 인기 피드 (리액션 기준)
     * 사용 예: "이번 주 인기 게시물", "오늘의 HOT 게시물"
     * @param startDate 시작 날짜 (예: 7일 전)
     * @param pageable 페이징 정보
     * @return 해당 기간 내 인기 피드
     */
    @Query("SELECT f FROM Feed f WHERE f.deletedAt IS NULL AND f.createdAt >= :startDate " +
            "ORDER BY f.reactionCount DESC, f.createdAt DESC")
    List<Feed> findPopularFeedsSince(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    // ========== 검색 ==========

    /**
     * 피드 내용으로 검색 (LIKE 검색)
     * 사용 예: 검색창에 "맛집" 입력 시 내용에 "맛집" 포함된 피드
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 피드 리스트
     */
    @Query("SELECT f FROM Feed f WHERE f.content LIKE %:keyword% AND f.deletedAt IS NULL")
    List<Feed> searchByContent(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 피드 내용 또는 태그로 검색
     * 사용 예: "여행" 검색 시 내용에 "여행" 포함 OR 태그에 "여행" 포함
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 피드 리스트
     */
    @Query("SELECT DISTINCT f FROM Feed f LEFT JOIN f.tags t WHERE " +
            "(f.content LIKE %:keyword% OR t LIKE %:keyword%) AND f.deletedAt IS NULL")
    List<Feed> searchByContentOrTag(@Param("keyword") String keyword, Pageable pageable);

    // ========== 통계 ==========

    /**
     * 전체 피드 개수 (삭제된 것 제외)
     * 사용 예: "전체 게시물 12,345개"
     */
    Long countByDeletedAtIsNull();

    /**
     * 특정 날짜 이후 생성된 피드 개수
     * 사용 예: "오늘 작성된 게시물", "이번 주 신규 게시물"
     * @param createdAt 기준 날짜
     * @return 해당 날짜 이후 피드 개수
     */
    Long countByCreatedAtAfterAndDeletedAtIsNull(LocalDateTime createdAt);
}
