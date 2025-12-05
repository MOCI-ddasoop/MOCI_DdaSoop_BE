package com.back.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 에러 응답 형식
 */
@Schema(description = "에러 응답")
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Schema(description = "에러 코드", example = "FEED001")
    private final String errorCode;

    @Schema(description = "에러 메시지", example = "피드를 찾을 수 없습니다.")
    private final String message;

    @Schema(description = "HTTP 상태 코드", example = "404")
    private final int status;

    @Schema(description = "발생 시각", example = "2024-01-01T12:00:00")
    private final LocalDateTime timestamp;

    /**
     * ErrorCode로부터 ErrorResponse 생성
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus().value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * ErrorCode와 커스텀 메시지로 ErrorResponse 생성
     */
    public static ErrorResponse of(ErrorCode errorCode, String customMessage) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(customMessage)
                .status(errorCode.getStatus().value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 메시지만으로 ErrorResponse 생성 (ErrorCode 없을 때)
     */
    public static ErrorResponse of(String message, int status) {
        return ErrorResponse.builder()
                .errorCode("ERROR")
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
