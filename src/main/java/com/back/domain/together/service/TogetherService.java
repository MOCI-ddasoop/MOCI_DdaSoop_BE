package com.back.domain.together.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.together.dto.TogetherDto;
import com.back.domain.together.entity.*;
import com.back.domain.together.repository.ParticipantsRepository;
import com.back.domain.together.repository.TogetherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TogetherService {
    private final TogetherRepository togetherRepository;
    private final MemberRepository memberRepository;
    private final ParticipantsRepository participantsRepository;

    // 최신순, 마감임박순, 인기순 통합
    public Page<TogetherDto.ListResponse> getAllTogether(
            List<TogetherCategory> categories, // 카데고리 다중 선택 가능
            TogetherMode mode,
            TogetherStatus status,
            TogetherSortType sortType,
            Pageable pageable
    ) {
        boolean hasCategory = categories != null && !categories.isEmpty();

        log.info("categories = {}", categories);
        log.info("hasCategory = {}", hasCategory);

        Page<Together> page;

        if (!hasCategory) {
            page = switch (sortType) {
                case POPULAR -> togetherRepository.findPopularWithoutCategory(mode, status, pageable);
                case DEADLINE -> togetherRepository.findDeadlineWithoutCategory(mode, status, pageable);
                default -> togetherRepository.findLatestWithoutCategory(mode, status, pageable);
            };
        } else {
            page = switch (sortType) {
                case POPULAR -> togetherRepository.findPopularWithCategory(categories, mode, status, pageable);
                case DEADLINE -> togetherRepository.findDeadlineWithCategory(categories, mode, status, pageable);
                default -> togetherRepository.findLatestWithCategory(categories, mode, status, pageable);
            };
        }

        return page.map(TogetherDto.ListResponse::from);
    }

    public TogetherDto.DetailResponse getTogether(Long id) {
        Together together = togetherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id+"번 함께하기 없음"));
        return TogetherDto.DetailResponse.from(together);
    }

    public TogetherDto.DescriptionResponse getTogetherDescription(Long id) {
        Together together = togetherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id+" 번 함께하기 설명하기 없음"));
        return TogetherDto.DescriptionResponse.from(together);
    }

    public TogetherDto.DetailResponse getTogetherByMemberId(Long memberId) {
        Together together = togetherRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원번호: "+memberId+" 번 함께하기 없음"));
        return TogetherDto.DetailResponse.from(together);
    }

    public TogetherDto.CreateResponse create(TogetherDto.CreateRequest request, Long memberId) {

        Member organizer = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원번호:"+memberId+"회원 없음"));

        Together together = Together.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .mode(request.mode())
                .capacity(request.capacity())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .member(organizer)
                .togetherStatus(TogetherStatus.RECRUITING)
                .build();

        return TogetherDto.CreateResponse.from(togetherRepository.save(together));
    }

    public String participate(Long togetherId, Long memberId) {

        // Together 조회
        Together together = togetherRepository.findById(togetherId)
                .orElseThrow(() -> new IllegalArgumentException("함께하기 번호: "+togetherId+"번 함께하기 없습니다."));
        if (together.getTogetherStatus() != TogetherStatus.RECRUITING) {
            throw new IllegalStateException("모집 중인 함께하기에만 참여할 수 있습니다.");
        }

        // Member 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원번호: "+memberId+" 번 회원 없습니다."));

        // Participants 조회
        Participants participants = participantsRepository.findByTogetherIdAndMemberId(togetherId, memberId)
                .orElse(null);

        // Participants가 없으면 새로 생성
        if (participants == null) {
            participantsRepository.save(Participants.create(together, member));
            return "참여가 완료되었습니다.";
        }

        participants.participate();
        return "참여가 완료되었습니다.";
    }

    @Transactional
    public String leave(Long togetherId, Long memberId) {

        // Participants 조회
        Participants participants = participantsRepository.findByTogetherIdAndMemberId(togetherId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("함께하기 번호: "+togetherId+" 번, 회원번호: "+memberId+" 번 참가자 정보가 없습니다."));

        participants.leave();
        return "탈퇴가 완료되었습니다.";
    }

    @Transactional
    public String drop(Long togetherId, Long targetId, Long requestId) {

        Together together = togetherRepository.findById(togetherId)
                .orElseThrow(() -> new IllegalArgumentException("함께하기 번호: "+togetherId+" 번 함께하기 없습니다."));

        // 함께하기 장 인지 확인
        if (!together.getMember().getId().equals(requestId)) {
            throw new IllegalStateException("함께하기 장만 강퇴할 수 있습니다.");
        }

        Participants participants = participantsRepository.findByTogetherIdAndMemberId(togetherId, targetId)
                .orElseThrow(() -> new IllegalArgumentException("함께하기 번호: "+togetherId+" 번, 회원번호: "+targetId+" 번 참가자 정보가 없습니다."));

        participants.drop();

        return "강퇴가 완료되었습니다.";
    }
}
