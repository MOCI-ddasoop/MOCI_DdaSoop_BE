# Member 도메인 테스트 코드 오류 수정 요약

## 📋 개요
Member 도메인의 컨트롤러 테스트 코드(`MemberControllerTest`, `AuthControllerTest`)에서 발생한 오류들을 분석하고 수정한 내용을 정리합니다.

---

## 🔍 발견된 문제점 및 해결 방법

### 1. **사용하지 않는 Import 제거**

#### 문제점
- 린터 경고: 사용하지 않는 import 문들이 다수 존재
- 코드 가독성 저하 및 불필요한 의존성

#### 해결 방법
**MemberControllerTest.java**
```java
// 제거된 import
- EmailCheckResponse
- MemberWithdrawResponse  
- NicknameCheckResponse
- Member, MemberRole, SocialProvider (엔티티 클래스들)
```

**AuthControllerTest.java**
```java
// 제거된 import
- LastLoginProviderResponse
- Member, MemberRole, SocialProvider
- HashMap, Map
- ObjectMapper (사용하지 않는 필드)
```

---

### 2. **Deprecated 어노테이션 경고 처리**

#### 문제점
- `@MockBean`이 Spring Boot 3.4.0부터 deprecated 되었으나 여전히 사용 중
- `OAuth2ClientAutoConfiguration`이 Spring Boot 3.5.0부터 deprecated
- 컴파일 경고 발생

#### 해결 방법
```java
// @MockBean 경고 억제
@SuppressWarnings("removal")
@MockBean
private MemberService memberService;

// OAuth2ClientAutoConfiguration 제거
@WebMvcTest(controllers = MemberController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
    // OAuth2ClientAutoConfiguration 제거됨
})
```

---

### 3. **GlobalExceptionHandler 미포함 문제** ⚠️ **핵심 문제**

#### 문제점
- `@WebMvcTest`는 슬라이스 테스트로 `@RestControllerAdvice`를 자동 스캔하지 않음
- 예외 처리 테스트에서 `GlobalExceptionHandler`가 작동하지 않아 예외가 제대로 처리되지 않음
- **6개 테스트 실패의 주요 원인**

#### 증상
```
MemberControllerTest > 2. 내 정보 조회 실패 - 회원을 찾을 수 없음 FAILED
MemberControllerTest > 4. 닉네임 중복 체크 - 사용 불가 FAILED
MemberControllerTest > 12. 회원 정보 수정 실패 - 중복된 이메일 FAILED
MemberControllerTest > 15. 회원 탈퇴 실패 - 회원을 찾을 수 없음 FAILED
AuthControllerTest > 2. 로그인 실패 - 회원을 찾을 수 없음 FAILED
AuthControllerTest > 4. Access Token 갱신 실패 - Refresh Token이 없음 FAILED
```

#### 해결 방법
```java
@WebMvcTest(controllers = MemberController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@Import(GlobalExceptionHandler.class)  // ✅ 명시적으로 GlobalExceptionHandler 포함
@ActiveProfiles("test")
public class MemberControllerTest {
    // ...
}
```

**이유**: `@WebMvcTest`는 컨트롤러 계층만 테스트하는 슬라이스 테스트이므로, 전역 예외 처리기(`@RestControllerAdvice`)를 수동으로 import해야 합니다.

---

### 4. **HTTP 상태 코드 불일치 문제** ⚠️ **핵심 문제**

#### 문제점
- 테스트에서 기대하는 상태 코드와 `ErrorCode` enum의 실제 상태 코드가 불일치
- `GlobalExceptionHandler`가 `ErrorCode`의 상태 코드를 사용하므로 테스트도 이를 반영해야 함

#### 상태 코드 매핑

| ErrorCode | 기대 상태 코드 (잘못됨) | 실제 상태 코드 (수정) | HTTP Status |
|-----------|---------------------|-------------------|-------------|
| `MEMBER_NOT_FOUND` | 400 (BAD_REQUEST) | 404 (NOT_FOUND) | ✅ 수정 |
| `MEMBER_EMAIL_DUPLICATE` | 400 (BAD_REQUEST) | 409 (CONFLICT) | ✅ 수정 |
| `AUTH_TOKEN_INVALID` | 400 (BAD_REQUEST) | 401 (UNAUTHORIZED) | ✅ 수정 |

#### 해결 방법

**Before (잘못된 코드)**
```java
@Test
void getMyInfo_fail_memberNotFound() throws Exception {
    // ...
    mockMvc.perform(get("/api/members/me"))
        .andExpect(status().isBadRequest())  // ❌ 400 기대
        .andExpect(jsonPath("$.message").exists());
}
```

**After (수정된 코드)**
```java
@Test
void getMyInfo_fail_memberNotFound() throws Exception {
    // ...
    mockMvc.perform(get("/api/members/me"))
        .andExpect(status().isNotFound())  // ✅ 404 기대
        .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."));
}
```

**수정된 테스트들**:
1. `getMyInfo_fail_memberNotFound()`: 400 → 404
2. `updateMember_fail_duplicateEmail()`: 400 → 409
3. `withdrawMember_fail_memberNotFound()`: 400 → 404
4. `login_fail_memberNotFound()`: 400 → 404
5. `refreshAccessToken_fail_noRefreshToken()`: 400 → 401
6. `refreshAccessToken_fail_invalidRefreshToken()`: 400 → 401

---

### 5. **유효성 검증 실패 - 닉네임 길이 제한**

#### 문제점
- 테스트 데이터 "existingNickname"이 17자로 `NicknameCheckRequest`의 `@Size(min = 2, max = 12)` 제약을 위반
- 유효성 검증에서 400 에러 발생하여 비즈니스 로직 테스트 불가

#### 해결 방법
```java
// Before
NicknameCheckRequest request = NicknameCheckRequest.builder()
    .nickname("existingNickname")  // ❌ 17자 (제한: 12자)
    .build();

// After
NicknameCheckRequest request = NicknameCheckRequest.builder()
    .nickname("existing")  // ✅ 8자 (제한: 2-12자)
    .build();
```

---

### 6. **Mockito 매칭 문제**

#### 문제점
- `Mockito.doThrow().when().updateMember(memberId, request)`에서 정확한 객체 인스턴스 매칭 실패
- 컨트롤러에서 생성한 `MemberUpdateRequest` 객체와 테스트에서 생성한 객체가 다른 인스턴스

#### 해결 방법
```java
// Before
Mockito.doThrow(new IllegalArgumentException("이미 사용 중인 이메일입니다."))
    .when(memberService)
    .updateMember(memberId, request);  // ❌ 정확한 인스턴스 매칭 실패

// After
Mockito.doThrow(new IllegalArgumentException("이미 사용 중인 이메일입니다."))
    .when(memberService)
    .updateMember(Mockito.eq(1L), Mockito.any(MemberUpdateRequest.class));  // ✅ any() 사용
```

**이유**: 
- 컨트롤러에서 JSON을 역직렬화하여 새로운 `MemberUpdateRequest` 인스턴스를 생성
- 테스트에서 생성한 객체와 다른 인스턴스이므로 `Mockito.any()` 사용 필요

---

## 📊 수정 전후 비교

### 수정 전
- ❌ 총 24개 테스트 중 6개 실패
- ❌ GlobalExceptionHandler 미포함으로 예외 처리 실패
- ❌ 상태 코드 불일치
- ❌ 유효성 검증 실패
- ❌ Mockito 매칭 실패

### 수정 후
- ✅ 총 24개 테스트 모두 통과
- ✅ GlobalExceptionHandler 정상 작동
- ✅ 상태 코드 일치
- ✅ 유효성 검증 통과
- ✅ Mockito 매칭 성공

---

## 🎯 핵심 교훈

### 1. **@WebMvcTest의 제한사항 이해**
- 슬라이스 테스트는 특정 계층만 로드하므로 전역 설정(`@RestControllerAdvice`)을 수동으로 포함해야 함
- `@Import` 어노테이션을 사용하여 필요한 컴포넌트를 명시적으로 추가

### 2. **ErrorCode와 테스트 일관성 유지**
- 테스트에서 기대하는 상태 코드는 실제 `ErrorCode` enum의 상태 코드와 일치해야 함
- `GlobalExceptionHandler`가 `ErrorCode`의 상태 코드를 사용하므로 테스트도 이를 반영

### 3. **Mockito 매칭 전략**
- 객체 인스턴스가 다른 경우 `Mockito.any()` 또는 `Mockito.any(Class.class)` 사용
- 정확한 값 매칭이 필요한 경우에만 `Mockito.eq()` 사용

### 4. **유효성 검증 제약사항 고려**
- 테스트 데이터는 실제 DTO의 유효성 검증 제약사항을 준수해야 함
- `@Size`, `@NotBlank`, `@Email` 등의 제약사항 확인 필요

---

## 📝 수정된 파일 목록

1. `src/test/java/com/back/domain/member/controller/MemberControllerTest.java`
   - 사용하지 않는 import 제거
   - `@Import(GlobalExceptionHandler.class)` 추가
   - 상태 코드 수정 (400 → 404/409)
   - 닉네임 길이 수정
   - Mockito 매칭 수정

2. `src/test/java/com/back/domain/member/controller/AuthControllerTest.java`
   - 사용하지 않는 import 제거
   - `@Import(GlobalExceptionHandler.class)` 추가
   - 상태 코드 수정 (400 → 401/404)
   - Mockito 매칭 수정

---

## ✅ 최종 검증 결과

```bash
./gradlew test --tests "com.back.domain.member.controller.*"

BUILD SUCCESSFUL
24 tests completed, 0 failed
```

**모든 테스트 통과!** ✅

