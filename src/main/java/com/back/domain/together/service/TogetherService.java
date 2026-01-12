package com.back.domain.together.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.together.dto.TogetherDto;
import com.back.domain.together.entity.Together;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherStatus;
import com.back.domain.together.repository.TogetherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TogetherService {
    private final TogetherRepository togetherRepository;
    private final MemberRepository memberRepository;

    public Page<TogetherDto.ListResponse> getAllTogether(
            TogetherCategory category,
            TogetherMode mode,
            TogetherStatus status,
            Pageable pageable
    ) {
        Page<Together> page;
        if (category == null && mode == null && status == null) {
            page = togetherRepository.findAll(pageable);
        } else if (category != null && mode == null && status == null) {
            page = togetherRepository.findByCategory(category, pageable);
        } else if (category == null && mode != null && status == null) {
            page = togetherRepository.findByMode(mode, pageable);
        } else if (category == null && mode == null && status != null) {
            page = togetherRepository.findByStatus(status, pageable);
        } else if (category != null && mode != null && status == null) {
            page = togetherRepository.findByCategoryAndMode(category, mode, pageable);
        } else if (category != null && mode == null) {
            page = togetherRepository.findByCategoryAndStatus(category, status, pageable);
        } else if (category == null) {
            page = togetherRepository.findByModeAndStatus(mode, status, pageable);
        } else {
            page = togetherRepository.findByCategoryAndModeAndStatus(category, mode, status, pageable);
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
                .status(TogetherStatus.RECRUITING)
                .build();

        return TogetherDto.CreateResponse.from(togetherRepository.save(together));
    }

}
