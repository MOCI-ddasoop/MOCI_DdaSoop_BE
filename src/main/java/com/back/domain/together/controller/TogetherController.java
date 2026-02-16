package com.back.domain.together.controller;

import com.back.domain.together.dto.TogetherDto;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherSortType;
import com.back.domain.together.entity.TogetherStatus;
import com.back.domain.together.service.TogetherService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/together")
@RequiredArgsConstructor
public class TogetherController {

    private final TogetherService togetherService;

    /**
     * SecurityContext에서 현재 로그인한 회원 ID 추출
     * @return 현재 로그인한 회원 ID
     */
    private Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        return (Long) authentication.getPrincipal();
    }

    @Operation(summary = "전체 함께하기 조회")
    @Description("전체 리스트 조회, 날짜,제목,카테고리, 온/오프, 모집중")
    @ApiResponse(
            responseCode = "200",
            description = "전체 함께하기 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.ListResponse.class))
    )
    @GetMapping("/list")
    @Transactional
    public ResponseEntity<RsData<TogetherDto.PageResponse<TogetherDto.ListResponse>>> getAllTogether(
            @RequestParam(required = false) List<TogetherCategory> categories,
            @RequestParam(required = false) TogetherMode mode,
            @RequestParam(required = false) TogetherStatus status,
            @RequestParam(defaultValue = "LATEST") TogetherSortType sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
            ) {
        Pageable pageable = PageRequest.of(
                page, size,
                switch (sortType){
                    case STATUS -> Sort.by("togetherStatus").descending();
                    case CATEGORY -> Sort.by("category").ascending();
                    case MODE -> Sort.by("mode").ascending();
                    default -> Sort.by("createdAt").descending();
                }
        );

        TogetherDto.PageResponse<TogetherDto.ListResponse> togetherPage = togetherService.getAllTogether(categories, mode, status, sortType, pageable);
        return ResponseEntity.ok().body(RsData.success("전체 함께하기 조회 성공", togetherPage));
    }

    @Operation(summary = "함께하기 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "함께하기 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.DetailResponse.class))
    )
    @GetMapping("/list/{id}")
    public ResponseEntity<RsData<TogetherDto.DetailResponse>> getTogether(
            @PathVariable Long id
    ) {
        TogetherDto.DetailResponse response = togetherService.getTogether(id);
        return ResponseEntity.ok().body(RsData.success("함께하기 상세 조회 성공", response));
    }

    @Operation(summary = "리스트 설명 조회")
    @ApiResponse(
            responseCode = "200",
            description = "함께하기 리스트 별 설명 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.DescriptionResponse.class))
    )
    @GetMapping("/list/{id}/description")
    public ResponseEntity<RsData<String>> getTogetherDescription(
            @PathVariable Long id
    ) {
        TogetherDto.DescriptionResponse response = togetherService.getTogetherDescription(id);
        return ResponseEntity.ok().body(RsData.success("함께하기 리스트 별 설명 조회 성공", response.description()));
    }

    @Operation(summary = "ID별 함께하기 조회")
    @Description("마이페이지용 ID별 함께하기 조회, 날짜,제목,카테고리, 온/오프, 모집중")
    @ApiResponse(
            responseCode = "200",
            description = "ID별 함께하기 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.DetailResponse.class))
    )
    @GetMapping("/member/{memberId}")
    public ResponseEntity<RsData<List<TogetherDto.DetailResponse>>> getTogetherByMemberId(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok().body(RsData.success("ID별 함께하기 조회 성공", togetherService.getTogetherByMemberId(memberId)));
    }

    @Operation(summary = "함께하기 참여 여부 조회")
    @Description("ID와 함께하기 번호로 사용자의 참여 여부 조회")
    @ApiResponse(
            responseCode = "200",
            description = "함께하기 참여 여부 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
    )
    @GetMapping("/{togetherId}/{memberId}/participation")
    public ResponseEntity<RsData<Boolean>> isParticipating(
            @PathVariable Long togetherId,
            @PathVariable Long memberId
    ) {
        Boolean isParticipating = togetherService.isParticipating(togetherId, memberId);
        return ResponseEntity.ok().body(RsData.success("함께하기 참여 여부 조회 성공", isParticipating));
    }


    //게시글 등록
    @Operation(summary = "함께하기 게시글 등록")
    @ApiResponse(
            responseCode = "201",
            description = "함께하기 게시글 등록 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.CreateResponse.class))
    )
    @PostMapping
    @Transactional
    public ResponseEntity<RsData<TogetherDto.CreateResponse>> create(
            @Valid @RequestBody TogetherDto.CreateRequest request
            ) {
        Long organizerId = getCurrentMemberId();
        TogetherDto.CreateResponse response = togetherService.create(request, organizerId);
        return ResponseEntity.status(201).body(RsData.success("함께하기 게시글이 등록되었습니다.", response));
    }

    //함께하기 참여
    @Operation(summary = "함께하기 참여")
    @ApiResponse(
            responseCode = "201",
            description = "함께하기 참여 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
    )
    @PostMapping("/{togetherId}/participate")
    @Transactional
    public ResponseEntity<RsData<String>> participate(
            @PathVariable Long togetherId
    ) {
        Long memberId = getCurrentMemberId();
        String rsData = togetherService.participate(togetherId, memberId);
        return ResponseEntity.status(201).body(RsData.success("함께하기 참여가 완료되었습니다.", rsData));
    }

    //
    @Operation(summary = "함께하기 탈퇴")
    @ApiResponse(
            responseCode = "201",
            description = "함께하기 탈퇴 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
    )
    @DeleteMapping("/{togetherId}/leave")
    @Transactional
    public ResponseEntity<RsData<String>> leave(
            @PathVariable Long togetherId
    ) {
        Long memberId = getCurrentMemberId();
        String rsData = togetherService.leave(togetherId, memberId);
        return ResponseEntity.ok().body(RsData.success("함께하기에서 탈퇴되었습니다.", rsData));
    }

    //
    @Operation(summary = "함께하기 강퇴")
    @ApiResponse(
            responseCode = "201",
            description = "함께하기 강퇴 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
    )
    @DeleteMapping("/{togetherId}/drop/{targetId}")
    @Transactional
    public ResponseEntity<RsData<String>> drop(
            @PathVariable Long togetherId,
            @PathVariable Long targetId
    ) {
        Long requestId = getCurrentMemberId();
        String rsData = togetherService.drop(togetherId, targetId, requestId);
        return ResponseEntity.ok().body(RsData.success("함께하기에서 강퇴되었습니다.", rsData));
    }
}
