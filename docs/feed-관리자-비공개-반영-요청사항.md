# Feed 도메인 수정 요청: 관리자 피드 삭제/비공개 시 메인 피드 미노출 반영

## 1. 배경

- **Admin 도메인**에서 관리자가 다음 작업을 할 수 있습니다.
  - 피드 **강제 삭제** (Soft Delete)
  - 피드 **비공개 처리** (`FeedVisibility.PRIVATE`)
- 이때 **메인 피드(일반 사용자용 목록/무한스크롤/검색)** 에는 삭제된 피드·비공개 피드가 **노출되지 않아야** 합니다.

## 2. 현재 상태 정리

| 구분 | 메인 반영 여부 | 비고 |
|------|----------------|------|
| **피드 삭제** | ✅ 이미 반영됨 | 목록/무한스크롤/검색 모두 `deletedAt.isNull()` 조건으로 제외되고 있음 |
| **피드 비공개** | ❌ 미반영 | `visibility` 조건이 없어서, 관리자가 비공개 처리한 피드도 메인에 그대로 노출됨 |

따라서 **비공개(PRIVATE) 피드를 메인 피드 목록에서 제외**하는 수정만 필요합니다.  
(삭제는 이미 잘 되어 있어서 수정 불필요)

## 3. 수정 요청 사항 (추가·변경할 부분)

아래는 **“메인에 노출되는 피드 = 전체 공개(PUBLIC)만”** 이 되도록 할 때 추가/수정하면 될 내용입니다.

---

### 3-1. `FeedRepositoryImpl` (QueryDSL)

**의도:** 메인용 무한스크롤·태그 검색·추천·제외 스크롤에서 **`visibility = PUBLIC`** 인 피드만 조회되도록 조건 추가.

| 메서드 | 추가할 조건 (기존 where에 and 로 추가) |
|--------|----------------------------------------|
| `findFeedsForInfiniteScroll` | `feed.visibility.eq(FeedVisibility.PUBLIC)` |
| `findByTagForInfiniteScroll` | `feed.visibility.eq(FeedVisibility.PUBLIC)` |
| `findRecommendedFeedsByTags` | `builder.and(feed.visibility.eq(FeedVisibility.PUBLIC));` (deletedAt 조건 바로 다음에) |
| `findFeedsForInfiniteScrollExcluding` | `builder.and(feed.visibility.eq(FeedVisibility.PUBLIC));` (deletedAt 조건 바로 다음에) |

- **참고:** `findMemberFeedsForInfiniteScroll`, `findTogetherFeedsForInfiniteScroll` 는 “특정 회원 피드” / “함께하기 내부” 용이므로 visibility 제한을 넣지 않아도 됩니다 (본인/멤버만 보는 목록이라 비공개 포함 여부는 기존 정책 유지).

---

### 3-2. `FeedRepository` (JPA)

**의도:** “댓글 많은 Top N”, “북마크 많은 Top N” 도 메인 노출용이므로 **전체 공개(PUBLIC)만** 조회되도록 변경.

- 기존 메서드 시그니처를 아래처럼 **파라미터 하나 추가**하는 형태로 변경해 주시면 됩니다.

```java
// 변경 전
List<Feed> findTop20ByDeletedAtIsNullOrderByCommentCountDescCreatedAtDesc();
List<Feed> findTop20ByDeletedAtIsNullOrderByBookmarkCountDescCreatedAtDesc();

// 변경 후 (FeedVisibility 파라미터 추가)
List<Feed> findTop20ByDeletedAtIsNullAndVisibilityOrderByCommentCountDescCreatedAtDesc(
        FeedVisibility visibility);
List<Feed> findTop20ByDeletedAtIsNullAndVisibilityOrderByBookmarkCountDescCreatedAtDesc(
        FeedVisibility visibility);
```

- `FeedVisibility` 는 `com.back.domain.feed.entity.FeedVisibility` 사용하시면 됩니다.

---

### 3-3. `FeedService`

**의도:** 위 Repository 변경에 맞춰, **메인 노출용** 호출 시에는 `FeedVisibility.PUBLIC` 을 넘기고, 인기 피드 검색 조건에도 PUBLIC 을 넣어 주세요.

1. **댓글 많은 피드 / 북마크 많은 피드 Top N**  
   - 새 시그니처 사용 시 `FeedVisibility.PUBLIC` 을 인자로 전달.
   - 예:  
     `findTop20ByDeletedAtIsNullAndVisibilityOrderByCommentCountDescCreatedAtDesc(FeedVisibility.PUBLIC)`  
     `findTop20ByDeletedAtIsNullAndVisibilityOrderByBookmarkCountDescCreatedAtDesc(FeedVisibility.PUBLIC)`

2. **인기 피드 (최근 7일)**  
   - `getPopularFeeds(int size)` 에서 사용하는 `FeedSearchCondition` 생성 시  
     `visibility(FeedVisibility.PUBLIC)` 를 한 번 설정해 주시면 됩니다.  
   - 예:  
     `FeedSearchCondition.builder()`  
     `.startDate(LocalDateTime.now().minusDays(7))`  
     **`.visibility(FeedVisibility.PUBLIC)`**  
     `.build()`

---

### 3-4. `FeedSearchCondition` (DTO)

**의도:** **페이징 검색(목록)** 기본 동작을 “메인용 = 전체 공개만” 으로 맞추기.

- `FeedSearchCondition.from(FeedSearchRequest request)` 안에서  
  기존 ` .visibility(null)` 를  
  **`.visibility(FeedVisibility.PUBLIC)`** 로 변경해 주시면 됩니다.  
- 이렇게 하면 클라이언트가 별도로 visibility 를 넘기지 않을 때, 메인 피드 목록에는 공개 피드만 노출됩니다.

---

## 4. 정리 (체크리스트)

- [ ] **FeedRepositoryImpl**  
  - `findFeedsForInfiniteScroll`  
  - `findByTagForInfiniteScroll`  
  - `findRecommendedFeedsByTags`  
  - `findFeedsForInfiniteScrollExcluding`  
  → 위 4곳에 `visibility.eq(FeedVisibility.PUBLIC)` 조건 추가

- [ ] **FeedRepository**  
  - 댓글 많은 Top20 / 북마크 많은 Top20 메서드에 `Visibility` 파라미터 추가 (위 시그니처 참고)

- [ ] **FeedService**  
  - 해당 Top N 호출 시 `FeedVisibility.PUBLIC` 전달  
  - `getPopularFeeds`용 `FeedSearchCondition` 에 `visibility(FeedVisibility.PUBLIC)` 설정

- [ ] **FeedSearchCondition.from()**  
  - `visibility(null)` → `visibility(FeedVisibility.PUBLIC)` 로 변경

이렇게 반영해 주시면, 관리자가 피드 삭제/비공개 처리한 내용이 메인 피드에 정상적으로 반영됩니다.  
추가로 필요한 조건(예: FOLLOWERS만 노출 등)이 있으면 그에 맞춰 visibility 조건만 조정하시면 됩니다.
