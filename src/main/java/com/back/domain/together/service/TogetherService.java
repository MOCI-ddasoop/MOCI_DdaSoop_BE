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
import org.springframework.data.domain.PageRequest;
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

    // 1페이지는 11개, 2페이지부터 12개
    // 최신순, 마감임박순, 인기순 통합
    public TogetherDto.PageResponse<TogetherDto.ListResponse> getAllTogether(
            List<TogetherCategory> categories, // 카데고리 다중 선택 가능
            TogetherMode mode,
            TogetherStatus status,
            TogetherSortType sortType,
            Pageable pageable
    ) {
        final int page = pageable.getPageNumber();
        final int size = pageable.getPageSize();
        final int firstPageRealSize = Math.max(0, size - 1); // 1페이지는 11개

        boolean hasCategory = categories != null && !categories.isEmpty();

        // 우리가 원하는 slice 범위
        final int start = (page == 0) ? 0 : (firstPageRealSize + (page - 1) * size);
        final int limit = (page == 0) ? firstPageRealSize : size;
        final int endExclusive = start + limit;

        PageRequest fetchPageable = PageRequest.of(0, Math.max(endExclusive, firstPageRealSize), pageable.getSort());

        Page<Together> fetched;
        if (!hasCategory) {
            fetched = switch (sortType) {
                case POPULAR -> togetherRepository.findPopularWithoutCategory(mode, status, fetchPageable);
                case DEADLINE -> togetherRepository.findDeadlineWithoutCategory(mode, status, fetchPageable);
                default -> togetherRepository.findLatestWithoutCategory(mode, status, fetchPageable);
            };
        } else {
            fetched = switch (sortType) {
                case POPULAR -> togetherRepository.findPopularWithCategory(categories, mode, status, fetchPageable);
                case DEADLINE -> togetherRepository.findDeadlineWithCategory(categories, mode, status, fetchPageable);
                default -> togetherRepository.findLatestWithCategory(categories, mode, status, fetchPageable);
            };
        }

        long totalElements = fetched.getTotalElements();
        int totalPages = calcTotalPagesUi(totalElements, size);

        List<Together> all = fetched.getContent();
        int safeFrom = Math.min(start, all.size());
        int safeTo = Math.min(endExclusive, all.size());
        List<TogetherDto.ListResponse> content = all.subList(safeFrom, safeTo)
                .stream()
                .map(TogetherDto.ListResponse::from)
                .toList();

        // 1페이지 첫번째에 가짜 카드 추가
        if (page == 0) {
            java.util.ArrayList<TogetherDto.ListResponse> tmp = new java.util.ArrayList<>(content.size() + 1);
            tmp.add(TogetherDto.ListResponse.fakeCard());
            tmp.addAll(content);
            content = tmp;
        }

        return new TogetherDto.PageResponse<>(content, page, size, totalElements, totalPages);
    }

    private int calcTotalPagesUi(long total, int size) {
        int firstPageRealSize = Math.max(0, size - 1);
        if (total <= firstPageRealSize) return 1;
        long remain = total - firstPageRealSize;
        long pagesAfter = (remain + size - 1) / size; // ceil
        return (int) (1 + pagesAfter);
    }

    public TogetherDto.DetailResponse getTogether(Long id) {
        Together together = togetherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id+"번 함께하기 없음"));
        return TogetherDto.DetailResponse.from(together);
    }

    public TogetherDto.DescriptionResponse getTogetherDescription(Long id) {
        Together together = togetherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id+" 번 함께하기 설명하기 없음"));

        String description = together.getDescription();

        return new TogetherDto.DescriptionResponse(
                description == null ? "" : description
        );
    }

    public List<TogetherDto.DetailResponse> getTogetherByMemberId(Long memberId) {
        List<Together> together = togetherRepository.findByMember_Id(memberId);
        return together.stream().map(TogetherDto.DetailResponse::from).toList();
    }

    // 마이페이지용 참가 & 소유 투게더 조회
    public List<TogetherDto.DetailResponse> getMemberIdOrParticipating(Long memberId){
        List<Together> togethers = togetherRepository.findAllByMemberIdOrParticipants_MemberId(memberId);
        return togethers.stream().map(TogetherDto.DetailResponse::from).toList();
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

    public Boolean isParticipating(Long togetherId, Long memberId) {
        Participants participants = participantsRepository.findByTogetherIdAndMemberId(togetherId, memberId)
                .orElse(null);
        return participants != null && participants.getParticipantsStatus() == ParticipantsStatus.PARTICIPATING;
    }
}
