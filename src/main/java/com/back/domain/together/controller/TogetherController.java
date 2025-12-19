package com.back.domain.together.controller;

import com.back.domain.together.dto.request.TogetherRequest;
import com.back.domain.together.dto.response.TogetherResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            content = @Content(schema = @Schema(implementation = TogetherResponse.class))
    )
    @GetMapping("/list")
    @Transactional
    public ResponseEntity<RsData<List<TogetherResponse>>> getAllTogether() {
        List<TogetherResponse> togetherList = togetherService.getAllTogether();
        return ResponseEntity.ok().body(RsData.success("전체 함께하기 조회 성공", togetherList));
    }

//    @Operation(summary = "ID별 함께하기 조회")
//    @Description("마이페이지용 ID별 함께하기 조회, 날짜,제목,카테고리, 온/오프, 모집중")
//    @ApiResponse(
//            responseCode = "200",
//            description = "ID별 함께하기 조회 성공",
//            content = @Content(schema = @Schema(implementation = TogetherResponse.class))
//    )
//    @GetMapping("/{memberId}") // 마이페이지속이라 아직 미정
//    public ResponseEntity<RsData<TogetherResponse>> getTogetherByMemberId(@PathVariable Long memberId) {
//        return ResponseEntity.ok().body(RsData.success("ID별 함께하기 조회 성공", togetherService.getTogetherByMemberId(memberId)));
//    }
//
    @Operation(summary = "함께하기 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "함께하기 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = TogetherResponse.class))
    )
    @GetMapping("/list/{id}")
    public ResponseEntity<RsData<TogetherResponse>> getTogether(
            @PathVariable Long id
    ) {
        TogetherResponse response = togetherService.getTogether(id);
        return ResponseEntity.ok().body(RsData.success("함께하기 상세 조회 성공", response));
    }


    //게시글 등록
    @Operation(summary = "함께하기 게시글 등록")
    @ApiResponse(
            responseCode = "201",
            description = "함께하기 게시글 등록 성공",
            content = @Content(schema = @Schema(implementation = TogetherRequest.class))
    )
    @PostMapping
    @Transactional
    public ResponseEntity<RsData<TogetherResponse>> create(
            @Valid @RequestBody TogetherRequest request
            ) {
        Long organizerId = 1L; // TODO: 인증 로직이 추가되면 수정 필요
        TogetherResponse response = togetherService.create(request, organizerId);
        return ResponseEntity.status(201).body(RsData.success("함께하기 게시글이 등록되었습니다.", response));
    }

}
