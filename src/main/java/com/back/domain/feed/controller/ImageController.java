package com.back.domain.feed.controller;

import com.back.domain.feed.dto.feed.response.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Tag(name = "Image", description = "이미지 업로드 API")
@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final RestTemplate restTemplate;

    @Value("${file.server.url:http://localhost:8000}")
    private String fileServerUrl;

    @Operation(
            summary = "이미지 업로드",
            description = "이미지를 업로드하고 URL을 반환합니다. 프론트엔드에서 미리보기 및 피드 생성에 사용할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업로드 성공",
                    content = @Content(schema = @Schema(implementation = ImageUploadResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일이 없거나 이미지가 아님)"),
            @ApiResponse(responseCode = "500", description = "업로드 실패")
    })
    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @Parameter(
                    description = "업로드할 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        // 1. 파일 검증
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 2. 이미지 파일인지 확인
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        // 3. UUID로 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        log.info("이미지 업로드 시작 - 원본: {}, 저장: {}", originalFilename, uniqueFilename);

        // 4. Python 파일 서버에 업로드
        String imageUrl = uploadToFileServer(file, uniqueFilename);

        // 5. 이미지 크기 추출
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        // 6. 응답 생성
        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageUrl(imageUrl)
                .width(width)
                .height(height)
                .fileSize(file.getSize())
                .originalFileName(originalFilename)
                .savedFileName(uniqueFilename)
                .build();

        log.info("이미지 업로드 완료 - URL: {}, 크기: {}x{}", imageUrl, width, height);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "다중 이미지 업로드",
            description = "여러 이미지를 한번에 업로드하고 URL 목록을 반환합니다. 최대 10개까지 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "업로드 성공",
                    content = @Content(schema = @Schema(implementation = List.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 개수 초과 등)"),
            @ApiResponse(responseCode = "500", description = "업로드 실패")
    })
    @PostMapping("/upload-multiple")
    public ResponseEntity<List<ImageUploadResponse>> uploadMultipleImages(
            @Parameter(
                    description = "업로드할 이미지 파일들 (최대 10개)",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {

        // 1. 파일 개수 검증
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        if (files.size() > 10) {
            throw new IllegalArgumentException("이미지는 최대 10개까지 업로드 가능합니다.");
        }

        log.info("다중 이미지 업로드 시작 - 파일 개수: {}", files.size());

        // 2. 각 파일 업로드
        List<ImageUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // 파일 검증
                if (file.isEmpty()) {
                    throw new IllegalArgumentException("파일이 비어있습니다.");
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
                }

                // UUID 파일명 생성
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String uniqueFilename = UUID.randomUUID().toString() + extension;

                // Python 파일 서버에 업로드
                String imageUrl = uploadToFileServer(file, uniqueFilename);

                // 이미지 크기 추출
                BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
                int width = bufferedImage.getWidth();
                int height = bufferedImage.getHeight();

                // 응답 생성
                ImageUploadResponse response = ImageUploadResponse.builder()
                        .imageUrl(imageUrl)
                        .width(width)
                        .height(height)
                        .fileSize(file.getSize())
                        .originalFileName(originalFilename)
                        .savedFileName(uniqueFilename)
                        .build();

                responses.add(response);

                log.info("이미지 업로드 완료 - {}: {}x{}", uniqueFilename, width, height);

            } catch (Exception e) {
                log.error("이미지 업로드 실패 - {}: {}", file.getOriginalFilename(), e.getMessage());
                throw new RuntimeException("이미지 업로드 실패: " + file.getOriginalFilename(), e);
            }
        }

        log.info("다중 이미지 업로드 완료 - 성공: {}/{}", responses.size(), files.size());

        return ResponseEntity.ok(responses);
    }

    /**
     * Python 파일 서버에 파일 업로드
     *
     * @param file 업로드할 파일
     * @param filename 저장할 파일명 (UUID)
     * @return 업로드된 파일의 URL
     */
    private String uploadToFileServer(MultipartFile file, String filename) throws IOException {
        // 1. MultipartFile을 Python 서버가 받을 수 있는 형태로 변환
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;  // UUID 파일명 사용
            }
        });

        // 2. HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        try {
            // 3. Python 서버로 POST 요청
            restTemplate.postForEntity(
                    fileServerUrl + "/file/",
                    requestEntity,
                    Void.class
            );

            // 4. Python 서버의 파일 URL 생성
            String imageUrl = fileServerUrl + "/uploads/" + filename;

            log.info("Python 파일 서버 업로드 성공 - {}", imageUrl);

            return imageUrl;

        } catch (Exception e) {
            log.error("Python 파일 서버 업로드 실패 - {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }
}
