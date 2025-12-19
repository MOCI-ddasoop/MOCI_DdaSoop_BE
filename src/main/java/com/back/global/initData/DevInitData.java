package com.back.global.initData;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.MemberRole;
import com.back.domain.member.entity.SocialProvider;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

/** 개발 환경 초기 데이터 설정 (JPA 테이블 생성 후 실행) */
@Slf4j
@Configuration
@Profile("default")
@RequiredArgsConstructor
public class DevInitData {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    /** 애플리케이션 준비 완료 후 샘플 데이터 생성 */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDataOnApplicationReady() {
        initMembers();
    }

    /** Member 샘플 데이터 생성 */
    public void initMembers() {
        // 이미 데이터가 있으면 스킵
        if (memberRepository.count() > 0) {
            log.info("Member 데이터가 이미 존재합니다. 초기 데이터 생성을 건너뜁니다.");
            return;
        }

        log.info("Member 샘플 데이터 생성 시작...");

        // 일반 사용자 1
        Member member1 = Member.builder()
                .name("홍길동")
                .nickname("hong123")
                .email("hong@example.com")
                .memberCode(memberService.generateMemberCode())
                .profileImageUrl("https://via.placeholder.com/150")
                .role(MemberRole.USER)
                .lastLoginProvider(SocialProvider.GOOGLE)
                .build();
        memberRepository.save(member1);
        log.info("Member 생성: {} (이메일: {})", member1.getNickname(), member1.getEmail());

        // 일반 사용자 2
        Member member2 = Member.builder()
                .name("김철수")
                .nickname("kim456")
                .email("kim@example.com")
                .memberCode(memberService.generateMemberCode())
                .profileImageUrl("https://via.placeholder.com/150")
                .role(MemberRole.USER)
                .lastLoginProvider(SocialProvider.KAKAO)
                .build();
        memberRepository.save(member2);
        log.info("Member 생성: {} (이메일: {})", member2.getNickname(), member2.getEmail());

        // 일반 사용자 3
        Member member3 = Member.builder()
                .name("이영희")
                .nickname("lee789")
                .email("lee@example.com")
                .memberCode(memberService.generateMemberCode())
                .role(MemberRole.USER)
                .lastLoginProvider(SocialProvider.NAVER)
                .build();
        memberRepository.save(member3);
        log.info("Member 생성: {} (이메일: {})", member3.getNickname(), member3.getEmail());

        // 관리자
        Member admin = Member.builder()
                .name("관리자")
                .nickname("admin")
                .email("admin@example.com")
                .memberCode(memberService.generateMemberCode())
                .profileImageUrl("https://via.placeholder.com/150")
                .role(MemberRole.ADMIN)
                .lastLoginProvider(SocialProvider.GOOGLE)
                .build();
        memberRepository.save(admin);
        log.info("Member 생성: {} (이메일: {}, 역할: {})", admin.getNickname(), admin.getEmail(), admin.getRole());

        log.info("Member 샘플 데이터 생성 완료 (총 {}개)", memberRepository.count());
    }
}
