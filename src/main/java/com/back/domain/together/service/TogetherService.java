package com.back.domain.together.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.together.dto.TogetherDto;
import com.back.domain.together.dto.request.TogetherRequest;
import com.back.domain.together.dto.response.TogetherResponse;
import com.back.domain.together.entity.Together;
import com.back.domain.together.entity.TogetherStatus;
import com.back.domain.together.repository.TogetherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TogetherService {
    private final TogetherRepository togetherRepository;
    private final MemberRepository memberRepository;

    public List<TogetherDto.ListResponse> getAllTogether() {
        return togetherRepository.findAll().stream()
                .map(TogetherDto.ListResponse::from)
                .toList();
    }

    public TogetherDto.DetailResponse getTogether(Long id) {
        Together together = togetherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id+"번 함께하기 없음"));
        return TogetherDto.DetailResponse.from(together);
    }

    public TogetherDto.DetailResponse getTogetherByMemberId(Long memberId) {
        Together together = togetherRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원번호:"+memberId+"번 함께하기 없음"));
        return TogetherDto.DetailResponse.from(together);
    }

    public TogetherResponse create(TogetherRequest request, Long organizerId) {

        Member organizer = memberRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("회원번호:"+organizerId+"회원 없음"));

        Together together = Together.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .mode(request.getMode())
                .capacity(request.getCapacity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .member(organizer)
                .status(TogetherStatus.RECRUITING)
                .build();

        return TogetherResponse.from(togetherRepository.save(together));
    }

}
