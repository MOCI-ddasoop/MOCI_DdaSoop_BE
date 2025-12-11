package com.back.domain.member.service;

import com.back.domain.member.dto.request.MemberUpdateRequest;
import com.back.domain.member.dto.response.MemberInfoResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public Member getMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.MEMBER_NOT_FOUND.getMessage()
            ));
    }

    public MemberInfoResponse getMemberInfo(Long memberId) {
        Member member = getMember(memberId);
        return MemberInfoResponse.from(member);
    }

    public boolean checkNickname(String nickname) {
        return memberRepository.existsByNicknameAndDeletedAtIsNull(nickname);
    }

    public boolean checkEmail(String email) {
        return memberRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Transactional
    public void updateMember(Long memberId, MemberUpdateRequest request) {
        Member member = getMember(memberId);

        if (request.getEmail() != null) {
            if (checkEmail(request.getEmail())) {
                throw new IllegalArgumentException(
                    ErrorCode.MEMBER_EMAIL_DUPLICATE.getMessage()
                );
            }
            member.updateEmail(request.getEmail());
        }

        if (request.getNickname() != null) {
            if (checkNickname(request.getNickname())) {
                throw new IllegalArgumentException(
                    ErrorCode.MEMBER_NICKNAME_DUPLICATE.getMessage()
                );
            }
            member.updateNickname(request.getNickname());
        }

        if (request.getProfileImageUrl() != null) {
            member.updateProfileImage(request.getProfileImageUrl());
        }

        memberRepository.save(member);
        log.info("회원 정보 수정 완료 - ID: {}", memberId);
    }

    @Transactional
    public void withdrawMember(Long memberId, String reason) {
        Member member = getMember(memberId);
        member.delete();
        memberRepository.save(member);
        log.info("회원 탈퇴 완료 - ID: {}, 사유: {}", memberId, reason);
    }
}


