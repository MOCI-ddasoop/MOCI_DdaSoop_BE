package com.back.domain.admin.service;

import com.back.domain.admin.dto.response.AdminMemberDetailResponse;
import com.back.domain.admin.dto.response.AdminMemberSummaryResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.MemberRole;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    public Page<AdminMemberSummaryResponse> getMemberPage(Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);
        return page.map(AdminMemberSummaryResponse::from);
    }

    public AdminMemberDetailResponse getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));
        return AdminMemberDetailResponse.from(member);
    }

    @Transactional
    public AdminMemberDetailResponse updateRole(Long memberId, String role) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));
        member.changeRole(MemberRole.valueOf(role));
        memberRepository.save(member);
        log.info("관리자 회원 역할 변경 - memberId: {}, role: {}", memberId, role);
        return AdminMemberDetailResponse.from(member);
    }

    @Transactional
    public void softDeleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));
        if (member.isDeleted()) {
            throw new IllegalArgumentException("이미 탈퇴 처리된 회원입니다.");
        }
        member.applyWithdrawnProfile();
        member.delete();
        memberRepository.save(member);
        log.info("관리자 회원 탈퇴 처리 - memberId: {}", memberId);
    }

    @Transactional
    public AdminMemberDetailResponse restoreMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));
        if (!member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴 상태가 아닌 회원입니다.");
        }
        member.restore();
        memberRepository.save(member);
        log.info("관리자 회원 복구 - memberId: {}", memberId);
        return AdminMemberDetailResponse.from(member);
    }
}
