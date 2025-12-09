package com.back.domain.member.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Size(min = 2, max = 12)
    @Column(name = "nickname", nullable = false, unique = true, length = 12)
    private String nickname;

    @Email
    @NotBlank
    @Size(max = 100)
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    // ========== 소셜 로그인 ==========
    
    /**
     * 회원이 연결한 소셜 계정 목록
     * 하나의 회원은 여러 소셜 계정을 가질 수 있습니다.
     * 예: Google로 가입 → 나중에 Kakao 계정도 연결
     */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberSocialAccount> socialAccounts = new ArrayList<>();

    /**
     * 최근 로그인한 소셜 로그인 제공자
     * 빠른 조회를 위해 Member 엔티티에 직접 저장합니다.
     * 
     * 사용 예시:
     * - "최근 로그인: 구글" 표시
     * - "마지막으로 Google로 로그인하셨습니다" 메시지
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_login_provider", length = 20)
    private SocialProvider lastLoginProvider;

    // ========== Soft Delete ==========
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;       // 삭제 시점 (null이면 삭제되지 않음)

    // ========== 비즈니스 로직 메서드 ==========
    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void changeRole(MemberRole newRole) {
        this.role = newRole;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ADMIN;
    }

    // ========== Soft Delete 메서드 ==========

    /**
     * 회원 탈퇴 (Soft Delete)
     * 데이터베이스에서 삭제하지 않고 deletedAt에 삭제 시점을 기록합니다.
     * 
     * 사용 예시:
     * Member member = memberRepository.findById(1L).orElseThrow();
     * member.delete();  // deletedAt = 현재 시간
     * memberRepository.save(member);
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 회원 복구
     * 탈퇴한 회원을 다시 활성화합니다.
     * 
     * 사용 예시:
     * Member member = memberRepository.findById(1L).orElseThrow();
     * member.restore();  // deletedAt = null
     * memberRepository.save(member);
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * 삭제 여부 확인
     * 
     * @return true: 탈퇴한 회원, false: 활성 회원
     * 
     * 사용 예시:
     * if (member.isDeleted()) {
     *     throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
     * }
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // ========== 소셜 로그인 메서드 ==========

    /**
     * 소셜 계정 추가
     * 
     * @param socialAccount 추가할 소셜 계정
     * 
     * 사용 예시:
     * MemberSocialAccount account = MemberSocialAccount.builder()
     *     .member(member)
     *     .provider(SocialProvider.GOOGLE)
     *     .providerId("google_user_123")
     *     .build();
     * member.addSocialAccount(account);
     */
    public void addSocialAccount(MemberSocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
        socialAccount.updateLastLogin();  // 최근 로그인 시간 업데이트
        this.lastLoginProvider = socialAccount.getProvider();  // 최근 로그인 방식 업데이트
    }

    /**
     * 최근 로그인 방식 업데이트
     * 소셜 계정으로 로그인할 때 호출됩니다.
     * 
     * @param provider 최근 로그인한 소셜 로그인 제공자
     * 
     * 사용 예시:
     * member.updateLastLoginProvider(SocialProvider.GOOGLE);
     */
    public void updateLastLoginProvider(SocialProvider provider) {
        this.lastLoginProvider = provider;
    }

    /**
     * 특정 소셜 로그인 제공자로 가입했는지 확인
     * 
     * @param provider 확인할 소셜 로그인 제공자
     * @return true: 해당 제공자로 가입함, false: 가입하지 않음
     * 
     * 사용 예시:
     * if (member.hasSocialAccount(SocialProvider.GOOGLE)) {
     *     // Google 계정이 연결되어 있음
     * }
     */
    public boolean hasSocialAccount(SocialProvider provider) {
        return this.socialAccounts.stream()
            .anyMatch(account -> account.getProvider() == provider);
    }
}
