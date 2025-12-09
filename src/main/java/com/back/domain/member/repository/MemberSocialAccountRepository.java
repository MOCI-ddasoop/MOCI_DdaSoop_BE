package com.back.domain.member.repository;

import com.back.domain.member.entity.MemberSocialAccount;
import com.back.domain.member.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MemberSocialAccount 엔티티를 위한 Repository 인터페이스
 * 
 * 소셜 로그인 계정 정보를 조회하는 메서드들을 제공합니다.
 */
public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    /**
     * 소셜 로그인 제공자와 제공자 ID로 소셜 계정 조회
     * 
     * 소셜 로그인 시 사용됩니다.
     * 예: Google 로그인 → provider=GOOGLE, providerId="google_user_123"
     * 
     * @param provider 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER 등)
     * @param providerId 소셜 로그인 제공자에서 발급한 고유 ID
     * @return Optional<MemberSocialAccount> (없으면 empty)
     * 
     * 사용 예시 (소셜 로그인):
     * Optional<MemberSocialAccount> account = memberSocialAccountRepository
     *     .findByProviderAndProviderId(SocialProvider.GOOGLE, "google_user_123");
     * 
     * if (account.isPresent()) {
     *     // 기존 회원 로그인
     *     Member member = account.get().getMember();
     * } else {
     *     // 신규 회원 가입
     * }
     */
    Optional<MemberSocialAccount> findByProviderAndProviderId(
        SocialProvider provider,
        String providerId
    );

    /**
     * 특정 회원의 소셜 계정 조회
     * 
     * @param memberId 회원 ID
     * @param provider 소셜 로그인 제공자
     * @return Optional<MemberSocialAccount> (없으면 empty)
     * 
     * 사용 예시:
     * Optional<MemberSocialAccount> account = memberSocialAccountRepository
     *     .findByMemberIdAndProvider(1L, SocialProvider.GOOGLE);
     * 
     * if (account.isPresent()) {
     *     // 해당 회원이 Google 계정을 가지고 있음
     * }
     */
    Optional<MemberSocialAccount> findByMemberIdAndProvider(
        Long memberId,
        SocialProvider provider
    );

    /**
     * 특정 소셜 계정이 이미 존재하는지 확인
     * 
     * @param provider 소셜 로그인 제공자
     * @param providerId 소셜 로그인 제공자에서 발급한 고유 ID
     * @return true: 이미 존재함, false: 존재하지 않음
     * 
     * 사용 예시 (중복 체크):
     * if (memberSocialAccountRepository.existsByProviderAndProviderId(
     *         SocialProvider.GOOGLE, "google_user_123")) {
     *     // 이미 가입된 계정
     * }
     */
    boolean existsByProviderAndProviderId(SocialProvider provider, String providerId);
}

