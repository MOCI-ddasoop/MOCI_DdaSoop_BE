package com.back.domain.member.service;

import com.back.domain.member.dto.OAuth2UserInfo;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.MemberRole;
import com.back.domain.member.entity.MemberSocialAccount;
import com.back.domain.member.entity.SocialProvider;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.repository.MemberSocialAccountRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 처리 서비스
 * 소셜 로그인 정보로 회원 조회/생성, null 값 허용 (나중에 추가 수집 가능)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginService {

    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberService memberService;

    /** 소셜 로그인 정보로 회원 조회 또는 생성 (null 값 허용) */
    @Transactional
    public Member findOrCreateMember(OAuth2UserInfo userInfo) {
        // 1. 기존 소셜 계정 조회
        MemberSocialAccount existingAccount = memberSocialAccountRepository
                .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .orElse(null);

        if (existingAccount != null) {
            // 기존 소셜 계정이 있으면 해당 회원 반환 및 정보 업데이트
            Member member = existingAccount.getMember();
            
            // 탈퇴한 회원인지 확인
            if (member.isDeleted()) {
                throw new IllegalArgumentException(
                        ErrorCode.MEMBER_ALREADY_DELETED.getMessage()
                );
            }

            // 소셜 로그인에서 받은 최신 정보로 업데이트 (프로필 이미지, 이름 등이 변경되었을 수 있음)
            updateMemberInfoFromSocialLogin(member, userInfo);

            // 최근 로그인 정보 업데이트
            existingAccount.updateLastLogin();
            member.updateLastLoginProvider(userInfo.getProvider());
            memberSocialAccountRepository.save(existingAccount);
            memberRepository.save(member);

            log.info("기존 소셜 계정으로 로그인 - Provider: {}, ProviderId: {}, MemberId: {}",
                    userInfo.getProvider(), userInfo.getProviderId(), member.getId());

            return member;
        }

        // 2. 소셜 계정이 없으면 새 회원 생성 (소셜 로그인으로만 가입 가능)
        return createNewMember(userInfo);
    }

    /** 소셜 로그인 최신 정보로 회원 정보 업데이트 */
    private void updateMemberInfoFromSocialLogin(Member member, OAuth2UserInfo userInfo) {
        // 프로필 이미지 업데이트 (소셜 로그인에서 받은 최신 이미지)
        if (userInfo.getProfileImageUrl() != null) {
            member.updateProfileImage(userInfo.getProfileImageUrl());
        }

        // 이름 업데이트 (소셜 로그인에서 받은 최신 이름)
        if (userInfo.getName() != null && !userInfo.getName().equals(member.getName())) {
            // 이름은 변경하지 않고 로그만 남김 (이름 변경은 별도 API로 처리)
            log.debug("소셜 로그인 이름 변경 감지 - 기존: {}, 새로운: {}", 
                    member.getName(), userInfo.getName());
        }
    }

    /** 새 회원 생성 (소셜 로그인 정보 저장, null 값 허용) */
    @Transactional
    private Member createNewMember(OAuth2UserInfo userInfo) {
        // 회원 코드 생성
        String memberCode = memberService.generateMemberCode();

        // 이름 생성 (소셜 로그인에서 받은 이름 사용, 없으면 기본값 생성)
        String name = userInfo.getName();
        if (name == null || name.isBlank()) {
            name = generateDefaultName(userInfo.getProvider());
        }

        // 닉네임: 소셜 로그인에서 받은 값은 무시하고 항상 null로 설정
        // (추가 정보 입력 페이지에서 사용자가 직접 입력하고 중복 검사를 받도록 강제)
        String nickname = null;

        // 이메일: 소셜 로그인에서 받은 값은 무시하고 항상 null로 설정
        // (추가 정보 입력 페이지에서 사용자가 직접 입력하고 중복 검사를 받도록 강제)
        String email = null;

        // 새 회원 생성 (nickname, email이 null일 수 있음 - 추가 정보 입력 페이지에서 입력받음)
        Member member = Member.builder()
                .name(name)
                .nickname(nickname)  // null 가능
                .email(email)        // null 가능
                .memberCode(memberCode)
                .profileImageUrl(userInfo.getProfileImageUrl())
                .role(MemberRole.USER)
                .lastLoginProvider(userInfo.getProvider())
                .build();

        // 소셜 계정 생성 및 연결 (소셜 로그인 정보 저장)
        MemberSocialAccount socialAccount = MemberSocialAccount.builder()
                .member(member)
                .provider(userInfo.getProvider())            // 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER)
                .providerId(userInfo.getProviderId())        // 소셜 로그인 제공자에서 발급한 고유 ID
                .build();

        member.addSocialAccount(socialAccount);

        // DB에 저장
        memberRepository.save(member);
        memberSocialAccountRepository.save(socialAccount);

        log.info("새 회원 생성 및 소셜 계정 연결 - Provider: {}, ProviderId: {}, MemberId: {}, Email: {}, Name: {}, Nickname: {}",
                userInfo.getProvider(), userInfo.getProviderId(), member.getId(), 
                member.getEmail() != null ? member.getEmail() : "(없음)", 
                member.getName() != null ? member.getName() : "(없음)",
                member.getNickname() != null ? member.getNickname() : "(없음)");

        return member;
    }

    /** 소셜 로그인에서 이름이 없을 때 기본값 생성 */
    private String generateDefaultName(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> "구글 사용자";
            case KAKAO -> "카카오 사용자";
            case NAVER -> "네이버 사용자";
        };
    }

    /** 고유한 닉네임 생성 (중복 시 숫자 추가) */
    private String generateUniqueNickname(String baseNickname) {
        // baseNickname이 null이거나 빈 문자열인 경우 기본값 사용
        if (baseNickname == null || baseNickname.isBlank()) {
            baseNickname = "사용자";
        }
        
        // 닉네임 길이 제한 (최대 12자)
        String nickname = baseNickname.length() > 12 
                ? baseNickname.substring(0, 12) 
                : baseNickname;

        // 중복되지 않으면 그대로 사용
        if (!memberService.checkNickname(nickname)) {
            return nickname;
        }

        // 중복되면 숫자 붙이기 (최대 9999까지 시도)
        for (int i = 1; i <= 9999; i++) {
            String candidate = nickname + i;
            // 길이 제한 확인 (12자 초과 시 앞부분 자르기)
            if (candidate.length() > 12) {
                int cutLength = 12 - String.valueOf(i).length();
                candidate = nickname.substring(0, cutLength) + i;
            }

            if (!memberService.checkNickname(candidate)) {
                return candidate;
            }
        }

        // 모든 시도 실패 시 랜덤 문자열 추가
        String randomSuffix = String.valueOf(System.currentTimeMillis()).substring(7);
        String finalNickname = nickname.substring(0, Math.min(nickname.length(), 12 - randomSuffix.length())) + randomSuffix;
        return finalNickname;
    }
}

