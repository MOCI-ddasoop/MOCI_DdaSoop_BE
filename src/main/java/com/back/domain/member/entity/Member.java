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
    @Size(min = 8, max = 10)
    @Column(name = "member_code", nullable = false, unique = true, length = 10, updatable = false)
    private String memberCode;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    // ========== 소셜 로그인 ==========
    
    /** 회원이 연결한 소셜 계정 목록 (하나의 회원은 여러 소셜 계정 보유 가능) */
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberSocialAccount> socialAccounts = new ArrayList<>();

    /** 최근 로그인한 소셜 로그인 제공자 */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_login_provider", length = 20)
    private SocialProvider lastLoginProvider;

    // ========== Soft Delete ==========
    /** 삭제 시점 (null이면 삭제되지 않음) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========== 비즈니스 로직 메서드 ==========
    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void updateEmail(String newEmail) {
        this.email = newEmail;
    }

    public void changeRole(MemberRole newRole) {
        this.role = newRole;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ADMIN;
    }

    // ========== Soft Delete 메서드 ==========

    /** 회원 탈퇴 (Soft Delete) - deletedAt에 삭제 시점 기록 */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 회원 복구 - deletedAt을 null로 설정하여 활성화 */
    public void restore() {
        this.deletedAt = null;
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // ========== 소셜 로그인 메서드 ==========

    /** 소셜 계정 추가 및 최근 로그인 정보 업데이트 */
    public void addSocialAccount(MemberSocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
        socialAccount.updateLastLogin();
        this.lastLoginProvider = socialAccount.getProvider();
    }

    /** 최근 로그인 방식 업데이트 */
    public void updateLastLoginProvider(SocialProvider provider) {
        this.lastLoginProvider = provider;
    }

    /** 특정 소셜 로그인 제공자로 가입했는지 확인 */
    public boolean hasSocialAccount(SocialProvider provider) {
        return this.socialAccounts.stream()
            .anyMatch(account -> account.getProvider() == provider);
    }
}
