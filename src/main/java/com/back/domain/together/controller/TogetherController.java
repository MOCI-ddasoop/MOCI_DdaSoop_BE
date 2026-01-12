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
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/together")
@RequiredArgsConstructor
public class TogetherController {

    private final TogetherService togetherService;

    @Operation(summary = "전체 함께하기 조회")
    @Description("전체 리스트 조회, 날짜,제목,카테고리, 온/오프, 모집중")
    @ApiResponse(
            responseCode = "200",
            description = "전체 함께하기 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
    )
    @GetMapping("/list")
    @Transactional
    public ResponseEntity<RsData<Page<TogetherDto.ListResponse>>> getAllTogether(
            @RequestParam(required = false) TogetherCategory category,
            @RequestParam(required = false) TogetherMode mode,
            @RequestParam(required = false) TogetherStatus status,
            @RequestParam(defaultValue = "LATEST") TogetherSortType sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ) {
        Pageable pageable = PageRequest.of(
                page, size,
                switch (sortType){
                    case STATUS -> Sort.by("status").descending();
                    case CATEGORY -> Sort.by("category").ascending();
                    case MODE -> Sort.by("mode").ascending();
                    default -> Sort.by("createdAt").descending();
                }
        );

        Page<TogetherDto.ListResponse> togetherPage = togetherService.getAllTogether(category, mode, status, pageable);
        return ResponseEntity.ok().body(RsData.success("전체 함께하기 조회 성공", togetherPage));
    }

    @Operation(summary = "함께하기 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "함께하기 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
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
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
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
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
    )
    @GetMapping("/member/{memberId}")
    public ResponseEntity<RsData<TogetherDto.DetailResponse>> getTogetherByMemberId(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok().body(RsData.success("ID별 함께하기 조회 성공", togetherService.getTogetherByMemberId(memberId)));
    }


    //게시글 등록
    @Operation(summary = "함께하기 게시글 등록")
    @ApiResponse(
            responseCode = "201",
            description = "함께하기 게시글 등록 성공",
            content = @Content(schema = @Schema(implementation = TogetherDto.class))
    )
    @PostMapping
    @Transactional
    public ResponseEntity<RsData<TogetherDto.CreateResponse>> create(
            @Valid @RequestBody TogetherDto.CreateRequest request
            ) {
        Long organizerId = 1L; // TODO: 인증 로직이 추가되면 수정 필요
        TogetherDto.CreateResponse response = togetherService.create(request, organizerId);
        return ResponseEntity.status(201).body(RsData.success("함께하기 게시글이 등록되었습니다.", response));
    }

}
