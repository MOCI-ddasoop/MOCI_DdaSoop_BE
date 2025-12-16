package com.back.domain.together.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
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

    public List<TogetherResponse> getAllTogether() {
        return togetherRepository.findAll().stream()
                .map(TogetherResponse::from)
                .toList();
    }

    public TogetherResponse getTogether(Long id) {
        Together together = togetherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("함께하기 없음"));
        return TogetherResponse.from(together);
    }

    public TogetherResponse create(TogetherRequest request, Long organizerId) {

        Member organizer = memberRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

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
