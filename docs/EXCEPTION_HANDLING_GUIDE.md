# 글로벌 예외 처리 사용 가이드

## 📋 **목차**
1. [개요](#1-개요)
2. [구조](#2-구조)
3. [사용 방법](#3-사용-방법)
4. [ErrorCode 목록](#4-errorcode-목록)
5. [try-catch 사용 케이스](#5-try-catch-사용-케이스)

---

## 1. 개요

### **기존 방식 (try-catch)**
```java
@PostMapping
public ResponseEntity<?> createFeed(@RequestBody FeedCreateRequest request) {
    try {
        Long feedId = feedService.createFeed(request);
        return ResponseEntity.ok(feedId);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(500).body("서버 오류");
    }
}
```
**문제점**:
- 모든 Controller에 try-catch 반복
- 에러 응답 형식이 제각각
- 에러 메시지 관리 어려움

---

### **새로운 방식 (GlobalExceptionHandler)**
```java
@PostMapping
public ResponseEntity<Long> createFeed(@RequestBody FeedCreateRequest request) {
    Long feedId = feedService.createFeed(request);
    return ResponseEntity.ok(feedId);
}
```
**장점**:
- try-catch 불필요 (GlobalExceptionHandler가 자동 처리)
- 일관된 에러 응답 형식
- 에러 코드 중앙 관리

---

## 2. 구조

### **파일 구성**
```
global/exception/
├── ErrorCode.java                  # 에러 코드 정의 (50개)
├── ErrorResponse.java              # API 에러 응답 형식
└── GlobalExceptionHandler.java     # 전역 예외 핸들러
```

### **처리 흐름**
```
1. Service에서 예외 발생
   throw new IllegalArgumentException(ErrorCode.FEED_NOT_FOUND.getMessage());
   
2. GlobalExceptionHandler가 자동으로 잡음
   @ExceptionHandler(IllegalArgumentException.class)
   
3. ErrorResponse 생성
   {
     "errorCode": "FEED001",
     "message": "피드를 찾을 수 없습니다.",
     "status": 404,
     "timestamp": "2024-12-05T16:00:00"
   }
   
4. 클라이언트에 반환
```

---

## 3. 사용 방법

### **3-1. Service 계층**

#### **기본 사용법**
```java
@Service
@RequiredArgsConstructor
public class FeedService {
    
    private final FeedRepository feedRepository;
    
    public FeedResponse getFeed(Long feedId) {
        // ✅ 이렇게 사용 (try-catch 없이)
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(
                    ErrorCode.FEED_NOT_FOUND.getMessage()
                ));
        
        return FeedResponse.from(feed);
    }
}
```

#### **권한 체크**
```java
public void deleteFeed(Long feedId, Long currentMemberId) {
    Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.FEED_NOT_FOUND.getMessage()
            ));
    
    // 권한 체크
    if (!feed.getMember().getId().equals(currentMemberId)) {
        throw new IllegalArgumentException(ErrorCode.FEED_FORBIDDEN.getMessage());
    }
    
    feed.delete();
}
```

#### **비즈니스 로직 검증**
```java
public void addImage(Long feedId, FeedImageRequest imageRequest) {
    Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.FEED_NOT_FOUND.getMessage()
            ));
    
    // 이미지 개수 체크
    if (feed.getImageCount() >= 10) {
        throw new IllegalArgumentException(
            ErrorCode.FEED_IMAGE_LIMIT_EXCEEDED.getMessage()
        );
    }
    
    // ... 이미지 추가 로직
}
```

---

### **3-2. Controller 계층**

#### **✅ 올바른 예시 (try-catch 없음)**
```java
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {
    
    private final FeedService feedService;
    
    // ✅ Good - try-catch 없음
    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeed(@PathVariable Long feedId) {
        FeedResponse response = feedService.getFeed(feedId);
        return ResponseEntity.ok(response);
    }
    
    // ✅ Good - try-catch 없음
    @PostMapping
    public ResponseEntity<Long> createFeed(@Valid @RequestBody FeedCreateRequest request) {
        Long feedId = feedService.createFeed(request, 1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedId);
    }
    
    // ✅ Good - try-catch 없음
    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(@PathVariable Long feedId) {
        feedService.deleteFeed(feedId, 1L);
        return ResponseEntity.noContent().build();
    }
}
```

#### **❌ 잘못된 예시 (불필요한 try-catch)**
```java
// ❌ Bad - try-catch 불필요
@GetMapping("/{feedId}")
public ResponseEntity<?> getFeed(@PathVariable Long feedId) {
    try {
        FeedResponse response = feedService.getFeed(feedId);
        return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

---

### **3-3. 에러 응답 형식**

#### **단순 에러**
```json
GET /api/feeds/999

Response: 400 Bad Request
{
  "errorCode": "FEED001",
  "message": "피드를 찾을 수 없습니다.",
  "status": 404,
  "timestamp": "2024-12-05T16:00:00"
}
```

#### **유효성 검증 에러 (@Valid)**
```json
POST /api/feeds
{
  "content": "",
  "images": [/* 11개 이미지 */]
}

Response: 400 Bad Request
{
  "errorCode": "COMMON001",
  "message": "내용은 필수입니다.",
  "status": 400,
  "timestamp": "2024-12-05T16:00:00"
}
```

---

## 4. ErrorCode 목록

### **4-1. 공통 에러 (COMMON)**
| 코드 | 메시지 | HTTP 상태 | 사용 예 |
|------|--------|-----------|---------|
| COMMON001 | 잘못된 입력값입니다. | 400 | @Valid 검증 실패 |
| COMMON002 | 잘못된 타입입니다. | 400 | 파라미터 타입 불일치 |
| COMMON003 | 필수 파라미터가 누락되었습니다. | 400 | @RequestParam 누락 |
| COMMON004 | 지원하지 않는 HTTP 메서드입니다. | 405 | GET으로 POST 호출 |
| COMMON005 | 서버 내부 오류가 발생했습니다. | 500 | 예상치 못한 오류 |
| COMMON006 | 인증이 필요합니다. | 401 | 로그인 필요 |
| COMMON007 | 접근 권한이 없습니다. | 403 | 권한 부족 |

### **4-2. 피드 에러 (FEED)**
| 코드 | 메시지 | HTTP 상태 | 사용 예 |
|------|--------|-----------|---------|
| FEED001 | 피드를 찾을 수 없습니다. | 404 | 존재하지 않는 피드 |
| FEED002 | 이미 삭제된 피드입니다. | 400 | 삭제된 피드 접근 |
| FEED003 | 피드에 대한 권한이 없습니다. | 403 | 작성자가 아닌 사람이 수정/삭제 |
| FEED004 | 이미지는 최대 10개까지 업로드 가능합니다. | 400 | 이미지 개수 초과 |
| FEED005 | 태그는 최대 30개까지 입력 가능합니다. | 400 | 태그 개수 초과 |
| FEED006 | 태그는 최대 50자까지 입력 가능합니다. | 400 | 태그 길이 초과 |
| FEED007 | 피드 내용은 최대 2000자까지 입력 가능합니다. | 400 | 내용 길이 초과 |
| FEED008 | 잘못된 피드 타입입니다. | 400 | 유효하지 않은 FeedType |
| FEED009 | 잘못된 공개 범위입니다. | 400 | 유효하지 않은 Visibility |

### **4-3. 댓글 에러 (COMMENT)**
| 코드 | 메시지 | HTTP 상태 | 사용 예 |
|------|--------|-----------|---------|
| COMMENT001 | 댓글을 찾을 수 없습니다. | 404 | 존재하지 않는 댓글 |
| COMMENT002 | 이미 삭제된 댓글입니다. | 400 | 삭제된 댓글 접근 |
| COMMENT003 | 댓글에 대한 권한이 없습니다. | 403 | 작성자가 아닌 사람이 수정/삭제 |
| COMMENT004 | 댓글은 최대 1000자까지 입력 가능합니다. | 400 | 댓글 길이 초과 |
| COMMENT005 | 대댓글에는 답글을 달 수 없습니다. | 400 | 대댓글의 대댓글 생성 시도 |

### **4-4. 회원 에러 (MEMBER)**
| 코드 | 메시지 | HTTP 상태 | 사용 예 |
|------|--------|-----------|---------|
| MEMBER001 | 회원을 찾을 수 없습니다. | 404 | 존재하지 않는 회원 |
| MEMBER002 | 이미 존재하는 회원입니다. | 409 | 중복 가입 시도 |
| MEMBER003 | 이미 탈퇴한 회원입니다. | 400 | 탈퇴한 회원 접근 |
| MEMBER004 | 회원 정보에 대한 권한이 없습니다. | 403 | 타인 정보 수정 시도 |

### **4-5. 함께하기 에러 (TOGETHER)**
| 코드 | 메시지 | HTTP 상태 | 사용 예 |
|------|--------|-----------|---------|
| TOGETHER001 | 함께하기를 찾을 수 없습니다. | 404 | 존재하지 않는 모임 |
| TOGETHER002 | 이미 참여 중인 함께하기입니다. | 409 | 중복 참여 시도 |
| TOGETHER003 | 정원이 가득 찼습니다. | 400 | 정원 초과 |
| TOGETHER004 | 이미 종료된 함께하기입니다. | 400 | 종료된 모임 접근 |
| TOGETHER005 | 함께하기에 대한 권한이 없습니다. | 403 | 관리자가 아닌 사람이 수정/삭제 |

### **4-6. 기타 에러**
| 도메인 | 개수 | 범위 |
|--------|------|------|
| REACTION | 2개 | REACTION001 ~ 002 |
| BOOKMARK | 2개 | BOOKMARK001 ~ 002 |
| FILE | 4개 | FILE001 ~ 004 |
| NOTIFICATION | 2개 | NOTIFICATION001 ~ 002 |

**전체 ErrorCode 확인**: `src/main/java/com/back/global/exception/ErrorCode.java`

---

## 5. try-catch 사용 케이스

### **5-1. try-catch를 사용해야 하는 경우**

#### **케이스 1: 외부 API 호출**
```java
@Service
@RequiredArgsConstructor
public class SlackService {
    
    private final SlackClient slackClient;
    
    // ✅ try-catch 사용 (외부 API 실패 시 계속 진행)
    public void sendNotification(String message) {
        try {
            slackClient.sendMessage(message);
            log.info("Slack 알림 전송 성공");
            
        } catch (Exception e) {
            // 실패해도 메인 로직은 계속 진행
            log.warn("Slack 알림 전송 실패 (무시): {}", e.getMessage());
        }
    }
}
```

#### **케이스 2: 파일 업로드/다운로드**
```java
@Service
@RequiredArgsConstructor
public class S3Service {
    
    private final AmazonS3 s3Client;
    
    // ✅ try-catch 사용 (IOException 처리)
    public String uploadImage(MultipartFile file) {
        String imageUrl = null;
        
        try {
            // S3 업로드
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            s3Client.putObject(bucketName, fileName, file.getInputStream(), null);
            imageUrl = s3Client.getUrl(bucketName, fileName).toString();
            
            log.info("이미지 업로드 성공: {}", fileName);
            
        } catch (IOException e) {
            log.error("이미지 업로드 실패: {}", e.getMessage());
            throw new IllegalArgumentException(ErrorCode.FILE_UPLOAD_FAILED.getMessage());
        }
        
        return imageUrl;
    }
}
```

#### **케이스 3: 여러 작업 중 일부 실패 허용**
```java
@Service
public class FeedService {
    
    // ✅ try-catch 사용 (일부 실패해도 계속 진행)
    public List<Long> deleteMultipleFeeds(List<Long> feedIds, Long memberId) {
        List<Long> failedIds = new ArrayList<>();
        
        for (Long feedId : feedIds) {
            try {
                deleteFeed(feedId, memberId);
                
            } catch (IllegalArgumentException e) {
                // 삭제 실패 시 해당 ID만 기록하고 계속 진행
                log.warn("피드 삭제 실패 - ID: {}, 사유: {}", feedId, e.getMessage());
                failedIds.add(feedId);
            }
        }
        
        return failedIds;  // 실패한 피드 ID 목록 반환
    }
}
```

#### **케이스 4: 트랜잭션 롤백 제어**
```java
@Service
@RequiredArgsConstructor
public class FeedService {
    
    private final S3Service s3Service;
    
    // ✅ try-catch 사용 (S3 업로드 실패 시 DB도 롤백)
    @Transactional
    public Long createFeedWithImage(FeedCreateRequest request, MultipartFile image) {
        String imageUrl = null;
        
        try {
            // 1. S3 업로드
            imageUrl = s3Service.upload(image);
            
            // 2. Feed 저장
            Feed feed = Feed.builder()
                    .content(request.getContent())
                    .build();
            
            feed.addImage(FeedImage.builder()
                    .imageUrl(imageUrl)
                    .build());
            
            return feedRepository.save(feed).getId();
            
        } catch (Exception e) {
            // S3 업로드 실패 → 전체 롤백
            log.error("피드 생성 실패: {}", e.getMessage());
            throw new IllegalArgumentException(ErrorCode.FILE_UPLOAD_FAILED.getMessage());
        }
    }
}
```

---

### **5-2. try-catch를 사용하지 않는 경우**

#### **❌ 잘못된 예시 1: 일반적인 비즈니스 로직**
```java
// ❌ Bad - try-catch 불필요
public FeedResponse getFeed(Long feedId) {
    try {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new IllegalArgumentException(
                    ErrorCode.FEED_NOT_FOUND.getMessage()
                ));
        return FeedResponse.from(feed);
    } catch (IllegalArgumentException e) {
        throw e;  // 그냥 다시 던지는 것은 의미 없음
    }
}

// ✅ Good - GlobalExceptionHandler가 처리
public FeedResponse getFeed(Long feedId) {
    Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.FEED_NOT_FOUND.getMessage()
            ));
    return FeedResponse.from(feed);
}
```

#### **❌ 잘못된 예시 2: Controller**
```java
// ❌ Bad - Controller에 try-catch 불필요
@GetMapping("/{feedId}")
public ResponseEntity<?> getFeed(@PathVariable Long feedId) {
    try {
        FeedResponse response = feedService.getFeed(feedId);
        return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

// ✅ Good - 예외 던지기만 하면 됨
@GetMapping("/{feedId}")
public ResponseEntity<FeedResponse> getFeed(@PathVariable Long feedId) {
    FeedResponse response = feedService.getFeed(feedId);
    return ResponseEntity.ok(response);
}
```

---

## 6. 실전 예시

### **예시 1: 피드 생성**
```java
@Service
@RequiredArgsConstructor
public class FeedService {
    
    private final FeedRepository feedRepository;
    private final TagService tagService;
    
    @Transactional
    public Long createFeed(FeedCreateRequest request, Long currentMemberId) {
        // 1. 태그 검증
        List<String> validatedTags = tagService.validateAndRefineTags(request.getTags());
        
        // 2. 이미지 개수 검증
        if (request.getImages() != null && request.getImages().size() > 10) {
            throw new IllegalArgumentException(
                ErrorCode.FEED_IMAGE_LIMIT_EXCEEDED.getMessage()
            );
        }
        
        // 3. Feed 생성
        Feed feed = Feed.builder()
                .content(request.getContent())
                .tags(validatedTags)
                .build();
        
        // 4. 저장
        return feedRepository.save(feed).getId();
    }
}
```

### **예시 2: 피드 수정 (권한 체크)**
```java
@Transactional
public void updateFeed(Long feedId, FeedUpdateRequest request, Long currentMemberId) {
    // 1. 피드 조회
    Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.FEED_NOT_FOUND.getMessage()
            ));
    
    // 2. 권한 체크
    if (!feed.getMember().getId().equals(currentMemberId)) {
        throw new IllegalArgumentException(ErrorCode.FEED_FORBIDDEN.getMessage());
    }
    
    // 3. 수정
    if (request.getContent() != null) {
        feed.updateContent(request.getContent());
    }
}
```

### **예시 3: 댓글 생성 (대댓글 제한)**
```java
@Transactional
public Long createComment(Long feedId, CommentCreateRequest request, Long currentMemberId) {
    // 1. 피드 조회
    Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.FEED_NOT_FOUND.getMessage()
            ));
    
    // 2. 부모 댓글 확인 (대댓글인 경우)
    Comment parent = null;
    if (request.getParentId() != null) {
        parent = commentRepository.findByIdAndDeletedAtIsNull(request.getParentId())
                .orElseThrow(() -> new IllegalArgumentException(
                    ErrorCode.COMMENT_NOT_FOUND.getMessage()
                ));
        
        // 3. 대댓글의 대댓글 방지
        if (parent.getParent() != null) {
            throw new IllegalArgumentException(
                ErrorCode.COMMENT_REPLY_NOT_ALLOWED.getMessage()
            );
        }
    }
    
    // 4. 댓글 생성
    Comment comment = Comment.builder()
            .feed(feed)
            .parent(parent)
            .content(request.getContent())
            .build();
    
    return commentRepository.save(comment).getId();
}
```

---

## 7. 요약

### **✅ DO (이렇게 하세요)**
1. Service에서 예외만 던지기
   ```java
   throw new IllegalArgumentException(ErrorCode.XXX.getMessage());
   ```

2. Controller에는 try-catch 쓰지 않기
   ```java
   FeedResponse response = feedService.getFeed(feedId);
   return ResponseEntity.ok(response);
   ```

3. ErrorCode 사용하기
   ```java
   ErrorCode.FEED_NOT_FOUND.getMessage()
   ```

### **❌ DON'T (이렇게 하지 마세요)**
1. Controller에 try-catch 쓰기
2. 에러 메시지 직접 작성하기
   ```java
   throw new IllegalArgumentException("피드를 찾을 수 없습니다.");  // ❌
   ```

3. 불필요한 try-catch
   ```java
   try {
       return service.method();
   } catch (Exception e) {
       throw e;  // ❌ 의미 없음
   }
   ```

### **🔧 try-catch 사용 케이스**
- 외부 API 호출 (Slack, 결제 등)
- 파일 업로드/다운로드 (S3, 로컬 파일)
- 여러 작업 중 일부 실패 허용
- 트랜잭션 롤백 제어

---

## 8. 참고 링크
- ErrorCode 전체 목록: `src/main/java/com/back/global/exception/ErrorCode.java`
- GlobalExceptionHandler: `src/main/java/com/back/global/exception/GlobalExceptionHandler.java`
- ErrorResponse: `src/main/java/com/back/global/exception/ErrorResponse.java`

---
