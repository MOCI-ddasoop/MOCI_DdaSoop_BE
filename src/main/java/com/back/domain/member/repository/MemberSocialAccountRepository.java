package com.back.domain.member.repository;

import com.back.domain.member.entity.MemberSocialAccount;
import com.back.domain.member.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** MemberSocialAccount 엔티티를 위한 Repository 인터페이스 */
public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    /** 소셜 로그인 제공자와 제공자 ID로 소셜 계정 조회 */
    Optional<MemberSocialAccount> findByProviderAndProviderId(
        SocialProvider provider,
        String providerId
    );

    /** 특정 회원의 소셜 계정 조회 */
    Optional<MemberSocialAccount> findByMemberIdAndProvider(
        Long memberId,
        SocialProvider provider
    );

    /** 특정 소셜 계정 존재 여부 확인 */
    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);
}

