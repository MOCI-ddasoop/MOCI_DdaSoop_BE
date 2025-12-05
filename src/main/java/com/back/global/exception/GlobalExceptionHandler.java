package com.back.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리 핸들러 (간소화 버전)
 * 모든 컨트롤러에서 발생하는 예외를 일관된 형식으로 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 검증 실패 (@RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        log.warn("Validation failed: {}", e.getMessage());
        
        // 첫 번째 에러 메시지만 반환 (간소화)
        String errorMessage = e.getBindingResult().getFieldErrors().isEmpty()
                ? ErrorCode.INVALID_INPUT_VALUE.getMessage()
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * @Valid 검증 실패 (@ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.warn("Bind failed: {}", e.getMessage());
        
        String errorMessage = e.getBindingResult().getFieldErrors().isEmpty()
                ? ErrorCode.INVALID_INPUT_VALUE.getMessage()
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 파라미터 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        log.warn("Type mismatch: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 필수 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        log.warn("Missing parameter: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.MISSING_REQUEST_PARAMETER);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 지원하지 않는 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e
    ) {
        log.warn("Method not allowed: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * IllegalArgumentException 처리
     * Service에서 throw new IllegalArgumentException(ErrorCode.XXX.getMessage()) 형태로 사용
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        
        // 메시지로 ErrorCode 찾기 시도
        ErrorCode errorCode = findErrorCodeByMessage(e.getMessage());
        
        ErrorResponse response = errorCode != null
                ? ErrorResponse.of(errorCode)
                : ErrorResponse.of(e.getMessage(), HttpStatus.BAD_REQUEST.value());
        
        return new ResponseEntity<>(response, 
                errorCode != null ? errorCode.getStatus() : HttpStatus.BAD_REQUEST);
    }

    /**
     * 그 외 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 메시지로 ErrorCode 찾기 (헬퍼 메서드)
     */
    private ErrorCode findErrorCodeByMessage(String message) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getMessage().equals(message)) {
                return errorCode;
            }
        }
        return null;
    }
}
