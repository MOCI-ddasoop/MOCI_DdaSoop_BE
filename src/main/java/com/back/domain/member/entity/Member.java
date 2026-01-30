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

    // 소셜 로그인에서 정보가 없을 수 있으므로 nullable (나중에 추가 수집 가능)
    @Size(min = 1, max = 50)
    @Column(name = "name", nullable = true, length = 50)
    private String name;

    @Size(min = 2, max = 12)
    @Column(name = "nickname", nullable = true, unique = true, length = 12)
    private String nickname;

    @Email
    @Size(max = 100)
    @Column(name = "email", nullable = true, unique = true, length = 100)
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

    /** Soft Delete */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /** 소셜 계정 추가 */
    public void addSocialAccount(MemberSocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
        socialAccount.updateLastLogin();
        this.lastLoginProvider = socialAccount.getProvider();
    }

    public void updateLastLoginProvider(SocialProvider provider) {
        this.lastLoginProvider = provider;
    }

    public boolean hasSocialAccount(SocialProvider provider) {
        return this.socialAccounts.stream()
            .anyMatch(account -> account.getProvider() == provider);
    }

    /** 추가 정보 입력 필요 여부 확인 (닉네임, 이메일 필수) */
    public boolean isAdditionalInfoRequired() {
        return nickname == null || nickname.isBlank() || 
               email == null || email.isBlank();
    }
}
