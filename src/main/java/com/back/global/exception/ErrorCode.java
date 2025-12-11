package com.back.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 공통 에러 (COMMON) ==========
    INVALID_INPUT_VALUE("COMMON001", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_VALUE("COMMON002", "잘못된 타입입니다.", HttpStatus.BAD_REQUEST),
    MISSING_REQUEST_PARAMETER("COMMON003", "필수 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("COMMON004", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_SERVER_ERROR("COMMON005", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED("COMMON006", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("COMMON007", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // ========== 피드 (FEED) ==========
    FEED_NOT_FOUND("FEED001", "피드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FEED_ALREADY_DELETED("FEED002", "이미 삭제된 피드입니다.", HttpStatus.BAD_REQUEST),
    FEED_FORBIDDEN("FEED003", "피드에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    FEED_IMAGE_LIMIT_EXCEEDED("FEED004", "이미지는 최대 10개까지 업로드 가능합니다.", HttpStatus.BAD_REQUEST),
    FEED_TAG_LIMIT_EXCEEDED("FEED005", "태그는 최대 30개까지 입력 가능합니다.", HttpStatus.BAD_REQUEST),
    FEED_TAG_LENGTH_EXCEEDED("FEED006", "태그는 최대 50자까지 입력 가능합니다.", HttpStatus.BAD_REQUEST),
    FEED_CONTENT_TOO_LONG("FEED007", "피드 내용은 최대 2000자까지 입력 가능합니다.", HttpStatus.BAD_REQUEST),
    FEED_INVALID_TYPE("FEED008", "잘못된 피드 타입입니다.", HttpStatus.BAD_REQUEST),
    FEED_INVALID_VISIBILITY("FEED009", "잘못된 공개 범위입니다.", HttpStatus.BAD_REQUEST),

    // ========== 댓글 (COMMENT) ==========
    COMMENT_NOT_FOUND("COMMENT001", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_ALREADY_DELETED("COMMENT002", "이미 삭제된 댓글입니다.", HttpStatus.BAD_REQUEST),
    COMMENT_FORBIDDEN("COMMENT003", "댓글에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    COMMENT_CONTENT_TOO_LONG("COMMENT004", "댓글은 최대 1000자까지 입력 가능합니다.", HttpStatus.BAD_REQUEST),
    COMMENT_REPLY_NOT_ALLOWED("COMMENT005", "대댓글에는 답글을 달 수 없습니다.", HttpStatus.BAD_REQUEST),

    // ========== 리액션 (REACTION) ==========
    REACTION_ALREADY_EXISTS("REACTION001", "이미 리액션을 누른 상태입니다.", HttpStatus.CONFLICT),
    REACTION_NOT_FOUND("REACTION002", "리액션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // ========== 북마크 (BOOKMARK) ==========
    BOOKMARK_ALREADY_EXISTS("BOOKMARK001", "이미 북마크한 상태입니다.", HttpStatus.CONFLICT),
    BOOKMARK_NOT_FOUND("BOOKMARK002", "북마크를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // ========== 파일 (FILE) ==========
    FILE_UPLOAD_FAILED("FILE001", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_SIZE_EXCEEDED("FILE002", "파일 크기가 제한을 초과했습니다.", HttpStatus.BAD_REQUEST),
    FILE_INVALID_EXTENSION("FILE003", "지원하지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND("FILE004", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // ========== 회원 (MEMBER) ==========
    MEMBER_NOT_FOUND("MEMBER001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MEMBER_ALREADY_DELETED("MEMBER002", "이미 탈퇴한 회원입니다.", HttpStatus.BAD_REQUEST),
    MEMBER_FORBIDDEN("MEMBER003", "회원 정보에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    MEMBER_EMAIL_DUPLICATE("MEMBER004", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    MEMBER_NICKNAME_DUPLICATE("MEMBER005", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    /**
     * 에러 코드로부터 ErrorCode enum 찾기
     * @param code 에러 코드 문자열
     * @return ErrorCode enum
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
