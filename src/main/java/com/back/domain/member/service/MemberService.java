package com.back.domain.member.service;

import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.feed.repository.FeedReactionRepository;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.member.dto.request.MemberUpdateRequest;
import com.back.domain.member.dto.response.MemberCountsResponse;
import com.back.domain.member.dto.response.MemberInfoResponse;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final FeedReactionRepository feedReactionRepository;
    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MEMBER_CODE_LENGTH = 8;

    public Member getMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.MEMBER_NOT_FOUND.getMessage()
            ));
    }

    public Member getMemberByCode(String memberCode) {
        return memberRepository.findByMemberCodeAndDeletedAtIsNull(memberCode)
            .orElseThrow(() -> new IllegalArgumentException(
                ErrorCode.MEMBER_NOT_FOUND.getMessage()
            ));
    }

    public String generateMemberCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder(MEMBER_CODE_LENGTH);

        for (int i = 0; i < MEMBER_CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        String generatedCode = code.toString();

        while (memberRepository.existsByMemberCodeAndDeletedAtIsNull(generatedCode)) {
            code = new StringBuilder(MEMBER_CODE_LENGTH);
            for (int i = 0; i < MEMBER_CODE_LENGTH; i++) {
                int index = random.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }
            generatedCode = code.toString();
        }

        return generatedCode;
    }

    public MemberInfoResponse getMemberInfo(Long memberId) {
        Member member = getMember(memberId);
        return MemberInfoResponse.from(member);
    }

    /** 회원 통계 정보 조회 (좋아요, 댓글, 피드 개수) */
    public MemberCountsResponse getMemberCounts(Long memberId) {
        // 회원 존재 여부 확인
        getMember(memberId);

        Long likedCount = feedReactionRepository.countByMemberId(memberId);
        Long commentedCount = commentRepository.countByMemberIdAndDeletedAtIsNull(memberId);
        Long feedCount = feedRepository.countByMemberIdAndDeletedAtIsNull(memberId);

        // null 체크 (count 메서드는 null을 반환할 수 있으므로 0으로 처리)
        likedCount = likedCount != null ? likedCount : 0L;
        commentedCount = commentedCount != null ? commentedCount : 0L;
        feedCount = feedCount != null ? feedCount : 0L;

        return MemberCountsResponse.builder()
                .likedCount(likedCount)
                .commentedCount(commentedCount)
                .feedCount(feedCount)
                .build();
    }

    public boolean checkNickname(String nickname) {
        return memberRepository.existsByNicknameAndDeletedAtIsNull(nickname);
    }

    public boolean checkEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return memberRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Transactional
    public void updateMember(Long memberId, MemberUpdateRequest request) {
        Member member = getMember(memberId);

        if (request.getEmail() != null) {
            if (member.getEmail() == null || !member.getEmail().equals(request.getEmail())) {
                if (checkEmail(request.getEmail())) {
                    throw new IllegalArgumentException(
                        ErrorCode.MEMBER_EMAIL_DUPLICATE.getMessage()
                    );
                }
                member.updateEmail(request.getEmail());
            }
        }

        if (request.getNickname() != null && !member.getNickname().equals(request.getNickname())) {
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
    }

    @Transactional
    public void withdrawMember(Long memberId, String reason) {
        Member member = getMember(memberId);

        if (member.isDeleted()) {
            throw new IllegalArgumentException(
                ErrorCode.MEMBER_ALREADY_DELETED.getMessage()
            );
        }

        // 탈퇴 시 닉네임/이메일 초기화 (재가입 시 최초 가입과 동일한 절차를 거치도록 함)
        member.updateNickname(null);
        member.updateEmail(null);
        // 이름·프로필 이미지 익명 처리 (다른 도메인에서 member.getName() 등 그대로 쓰면 "탈퇴한 회원"으로 노출)
        member.applyWithdrawnProfile();

        member.delete();
        memberRepository.save(member);
    }

    /** 추가 정보 입력 완료 (닉네임, 이메일 필수 입력) */
    @Transactional
    public void completeAdditionalInfo(Long memberId, String nickname, String email) {
        Member member = getMember(memberId);

        // 이미 추가 정보가 입력된 경우
        if (!member.isAdditionalInfoRequired()) {
            return;
        }

        // 닉네임 업데이트 및 중복 체크
        if (nickname != null && !nickname.isBlank()) {
            // 현재 닉네임과 다르거나 null인 경우에만 체크
            if (member.getNickname() == null || !member.getNickname().equals(nickname)) {
                if (checkNickname(nickname)) {
                    throw new IllegalArgumentException(
                        ErrorCode.MEMBER_NICKNAME_DUPLICATE.getMessage()
                    );
                }
                member.updateNickname(nickname);
            }
        }

        // 이메일 업데이트 및 중복 체크
        if (email != null && !email.isBlank()) {
            // 현재 이메일과 다르거나 null인 경우에만 체크
            if (member.getEmail() == null || !member.getEmail().equals(email)) {
                if (checkEmail(email)) {
                    throw new IllegalArgumentException(
                        ErrorCode.MEMBER_EMAIL_DUPLICATE.getMessage()
                    );
                }
                member.updateEmail(email);
            }
        }

        memberRepository.save(member);
    }
}


