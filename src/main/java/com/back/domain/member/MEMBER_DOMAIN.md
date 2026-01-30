# Member 도메인 상세 문서

## 목차
1. [개요](#개요)
2. [엔티티 구조](#엔티티-구조)
3. [Repository 메서드](#repository-메서드)
4. [Service 메서드](#service-메서드)
5. [Controller API](#controller-api)
6. [주요 기능 상세](#주요-기능-상세)

---

## 개요

Member 도메인은 회원 정보 관리 및 소셜 로그인 기능을 담당합니다.

### 주요 기능
- 회원 정보 조회/수정/탈퇴
- 소셜 로그인 계정 관리 (Google, Kakao, Naver 등)
- 이메일/닉네임 중복 체크
- Soft Delete를 통한 회원 탈퇴 처리

---

## 엔티티 구조

### Member 엔티티

#### 필드
- `name`: 회원 이름 (1-50자)
- `nickname`: 닉네임 (2-12자, 유니크)
- `email`: 이메일 (최대 100자, 유니크)
- `memberCode`: 회원 고유번호 (8-10자, 유니크, 수정 불가)
- `profileImageUrl`: 프로필 이미지 URL (최대 500자)
- `role`: 회원 권한 (USER, ADMIN)
- `socialAccounts`: 연결된 소셜 계정 목록 (OneToMany)
- `lastLoginProvider`: 최근 로그인한 소셜 로그인 제공자
- `deletedAt`: 삭제 시점 (Soft Delete용, null이면 활성 회원)

#### 주요 메서드

**Soft Delete 메서드**
- `delete()`: 회원 탈퇴 (Soft Delete) - deletedAt에 삭제 시점 기록
  ```java
  Member member = memberRepository.findById(1L).orElseThrow();
  member.delete();  // deletedAt = 현재 시간
  memberRepository.save(member);
  ```
- `restore()`: 회원 복구 - deletedAt을 null로 설정하여 활성화
  ```java
  Member member = memberRepository.findById(1L).orElseThrow();
  member.restore();  // deletedAt = null
  memberRepository.save(member);
  ```
- `isDeleted()`: 삭제 여부 확인
  ```java
  if (member.isDeleted()) {
      throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
  }
  ```

**소셜 로그인 메서드**
- `addSocialAccount(MemberSocialAccount socialAccount)`: 소셜 계정 추가 및 최근 로그인 정보 업데이트
  ```java
  MemberSocialAccount account = MemberSocialAccount.builder()
      .member(member)
      .provider(SocialProvider.GOOGLE)
      .providerId("google_user_123")
      .build();
  member.addSocialAccount(account);
  ```
- `updateLastLoginProvider(SocialProvider provider)`: 최근 로그인 방식 업데이트
  ```java
  member.updateLastLoginProvider(SocialProvider.GOOGLE);
  ```
- `hasSocialAccount(SocialProvider provider)`: 특정 소셜 로그인 제공자로 가입했는지 확인
  ```java
  if (member.hasSocialAccount(SocialProvider.GOOGLE)) {
      // Google 계정이 연결되어 있음
  }
  ```

### MemberSocialAccount 엔티티

#### 필드
- `member`: 소셜 계정을 소유한 회원 (ManyToOne)
- `provider`: 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER 등)
- `providerId`: 소셜 로그인 제공자에서 발급한 고유 ID
- `lastLoginAt`: 이 소셜 계정으로 마지막 로그인한 시각

#### 특징
- 하나의 Member는 여러 소셜 계정을 가질 수 있음
  - 예: Google로 가입 → 나중에 Kakao 계정도 연결
- `provider`와 `providerId`의 조합은 유일해야 함
  - 같은 Google 계정이 여러 Member에 연결되는 것을 방지

#### 주요 메서드
- `updateLastLogin()`: 소셜 계정으로 로그인 시 lastLoginAt을 현재 시간으로 업데이트
  ```java
  MemberSocialAccount account = memberSocialAccountRepository.findByProviderAndProviderId(...);
  account.updateLastLogin();  // lastLoginAt = 현재 시간
  memberSocialAccountRepository.save(account);
  ```

---

## Repository 메서드

### MemberRepository

#### 기본 조회 (Soft Delete 고려)
모든 조회 메서드는 `deletedAtIsNull` 조건을 포함하여 탈퇴한 회원을 제외합니다.

- `findByIdAndDeletedAtIsNull(Long id)`: ID로 활성 회원 조회
  ```java
  Member member = memberRepository.findByIdAndDeletedAtIsNull(1L)
      .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
  ```

- `findByEmailAndDeletedAtIsNull(String email)`: 이메일로 활성 회원 조회
  ```java
  Member member = memberRepository.findByEmailAndDeletedAtIsNull("hong@example.com")
      .orElseThrow(() -> new IllegalArgumentException("이메일이 없습니다."));
  ```

- `findByNicknameAndDeletedAtIsNull(String nickname)`: 닉네임으로 활성 회원 조회
  ```java
  boolean exists = memberRepository.findByNicknameAndDeletedAtIsNull("홍길동")
      .isPresent();  // true면 이미 사용 중인 닉네임
  ```

- `findByMemberCodeAndDeletedAtIsNull(String memberCode)`: 고유번호로 활성 회원 조회
  ```java
  Member member = memberRepository.findByMemberCodeAndDeletedAtIsNull("ABC123XYZ")
      .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
  ```

#### 존재 여부 확인 (중복 체크용)
- `existsByEmailAndDeletedAtIsNull(String email)`: 이메일 중복 체크 (활성 회원만)
  ```java
  if (memberRepository.existsByEmailAndDeletedAtIsNull("hong@example.com")) {
      throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
  }
  ```

- `existsByNicknameAndDeletedAtIsNull(String nickname)`: 닉네임 중복 체크 (활성 회원만)
  ```java
  if (memberRepository.existsByNicknameAndDeletedAtIsNull("홍길동")) {
      throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
  }
  ```

- `existsByMemberCodeAndDeletedAtIsNull(String memberCode)`: 고유번호 중복 체크 (활성 회원만)
  ```java
  if (memberRepository.existsByMemberCodeAndDeletedAtIsNull("ABC123XYZ")) {
      // 중복이므로 새로운 고유번호 생성 필요
  }
  ```

#### 관리자용 메서드 (탈퇴 여부 무관)
- `findByEmail(String email)`: 이메일로 회원 조회 (탈퇴한 회원도 포함)
  - 주의: 일반적인 로그인/조회에는 사용하지 말고, 관리자 기능에서만 사용
  - 일반 조회는 `findByEmailAndDeletedAtIsNull()`을 사용
  ```java
  Optional<Member> member = memberRepository.findByEmail("hong@example.com");
  ```

- `countByDeletedAtIsNull()`: 활성 회원 수 조회
  ```java
  long activeMemberCount = memberRepository.countByDeletedAtIsNull();
  System.out.println("현재 활성 회원 수: " + activeMemberCount);
  ```

### MemberSocialAccountRepository

- `findByProviderAndProviderId(SocialProvider provider, String providerId)`: 소셜 로그인 제공자와 제공자 ID로 소셜 계정 조회
  ```java
  Optional<MemberSocialAccount> account = memberSocialAccountRepository
      .findByProviderAndProviderId(SocialProvider.GOOGLE, "google_user_123");
  
  if (account.isPresent()) {
      // 기존 회원 로그인
      Member member = account.get().getMember();
  } else {
      // 신규 회원 가입
  }
  ```

- `findByMemberIdAndProvider(Long memberId, SocialProvider provider)`: 특정 회원의 소셜 계정 조회
  ```java
  Optional<MemberSocialAccount> account = memberSocialAccountRepository
      .findByMemberIdAndProvider(1L, SocialProvider.GOOGLE);
  
  if (account.isPresent()) {
      // 해당 회원이 Google 계정을 가지고 있음
  }
  ```

- `existsByProviderAndProviderId(SocialProvider provider, String providerId)`: 특정 소셜 계정 존재 여부 확인
  ```java
  if (memberSocialAccountRepository.existsByProviderAndProviderId(
          SocialProvider.GOOGLE, "google_user_123")) {
      // 이미 가입된 계정
  }
  ```

---

## Service 메서드

### MemberService

- `getMember(Long memberId)`: ID로 회원 조회
- `getMemberByCode(String memberCode)`: 고유번호로 회원 조회
- `generateMemberCode()`: 회원 고유번호 생성 (8자리 영문+숫자 조합, 중복 체크 포함)
- `getMemberInfo(Long memberId)`: 회원 정보 조회 (DTO 반환)
- `checkNickname(String nickname)`: 닉네임 중복 체크
- `checkEmail(String email)`: 이메일 중복 체크
- `updateMember(Long memberId, MemberUpdateRequest request)`: 회원 정보 수정
  - 이메일, 닉네임, 프로필 이미지 수정 가능
  - 중복 체크 후 업데이트
- `withdrawMember(Long memberId, String reason)`: 회원 탈퇴 (Soft Delete)
  - 이미 탈퇴한 회원인지 확인 후 처리

---

## Controller API

### MemberController

#### GET `/api/members/me`
- **기능**: 내 정보 조회
- **인증**: TODO - 인증 연결 후 `@AuthenticationPrincipal CustomUserDetails userDetails` 사용
- **현재**: 임시로 `currentMemberId = 1L` 사용

#### POST `/api/members/check-nickname`
- **기능**: 닉네임 중복 체크
- **요청**: `NicknameCheckRequest`
- **응답**: `NicknameCheckResponse` (available/unavailable)

#### POST `/api/members/check-email`
- **기능**: 이메일 중복 체크
- **요청**: `EmailCheckRequest`
- **응답**: `EmailCheckResponse` (available/unavailable)

#### PUT `/api/members/me`
- **기능**: 회원 정보 수정
- **인증**: TODO - 인증 연결 후 `@AuthenticationPrincipal CustomUserDetails userDetails` 사용
- **현재**: 임시로 `currentMemberId = 1L` 사용
- **요청**: `MemberUpdateRequest` (email, nickname, profileImageUrl)
- **에러**: 409 - 중복된 이메일 또는 닉네임

#### DELETE `/api/members/me`
- **기능**: 회원 탈퇴 (Soft Delete)
- **인증**: TODO - 인증 연결 후 `@AuthenticationPrincipal CustomUserDetails userDetails` 사용
- **현재**: 임시로 `currentMemberId = 1L` 사용
- **요청**: `MemberWithdrawRequest` (reason - 선택)
- **응답**: `MemberWithdrawResponse`

### AuthController

#### GET `/api/auth/last-login-provider`
- **기능**: 최근 로그인 방식 조회
- **설명**: 세션에 저장된 최근 로그인 방식을 조회합니다. 로그인하지 않은 상태에서도 이전 로그인 기록이 있으면 표시됩니다.
- **응답**: `LastLoginProviderResponse`

---

## 주요 기능 상세

### Soft Delete
- 회원 탈퇴 시 데이터베이스에서 실제로 삭제하지 않고 `deletedAt` 필드에 삭제 시점을 기록
- 모든 조회 메서드는 `deletedAtIsNull` 조건을 포함하여 탈퇴한 회원을 제외
- 관리자 기능에서만 탈퇴한 회원도 조회 가능

### 소셜 로그인
- 하나의 회원은 여러 소셜 계정을 가질 수 있음
- 최근 로그인한 소셜 제공자 정보를 `Member.lastLoginProvider`에 저장하여 빠른 조회 가능
- 각 소셜 계정의 마지막 로그인 시간을 `MemberSocialAccount.lastLoginAt`에 기록

### 회원 고유번호 생성
- 8자리 영문 대문자 + 숫자 조합으로 생성
- 중복 체크 후 유일한 번호가 생성될 때까지 재시도

### 인증 연동 준비
- 현재는 임시로 `currentMemberId = 1L` 사용
- 인증 연결 후 `@AuthenticationPrincipal CustomUserDetails userDetails`를 사용하도록 주석 처리되어 있음

