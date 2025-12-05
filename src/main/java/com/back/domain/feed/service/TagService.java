package com.back.domain.feed.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 태그 검증 및 정제 서비스
 * 프론트엔드에서 content와 별도로 tags 배열을 받는 경우 사용
 */
@Slf4j
@Service
public class TagService {
    
    // 태그 최대 길이
    private static final int MAX_TAG_LENGTH = 50;
    
    // 태그 최대 개수
    private static final int MAX_TAG_COUNT = 30;

    /**
     * 태그 검증 및 정제
     * @param tags 프론트에서 받은 태그 목록
     * @return 정제된 태그 목록
     */
    public List<String> validateAndRefineTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> refinedTags = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .map(this::sanitizeTag)  // 특수문자 제거
                .filter(tag -> !tag.isBlank())  // sanitize 후 빈 문자열 제거
                .filter(tag -> tag.length() <= MAX_TAG_LENGTH)
                .distinct()  // 중복 제거
                .limit(MAX_TAG_COUNT)  // 최대 개수 제한
                .collect(Collectors.toList());

        log.debug("태그 검증 완료 - 입력: {}, 정제 후: {}", tags.size(), refinedTags.size());
        return refinedTags;
    }

    /**
     * 태그에서 특수문자 제거 및 정규화
     * @param tag 원본 태그
     * @return 정제된 태그
     */
    private String sanitizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        
        // 1. 앞뒤 공백 제거
        String sanitized = tag.trim();
        
        // 2. # 제거 (만약 프론트에서 #포함해서 보낸 경우)
        sanitized = sanitized.replaceAll("^#+", "");
        
        // 3. 한글, 영문, 숫자, 언더스코어만 허용
        sanitized = sanitized.replaceAll("[^가-힣a-zA-Z0-9_]", "");
        
        return sanitized;
    }

    /**
     * 태그 개수 검증
     * @param tagCount 태그 개수
     * @return 유효 여부
     */
    public boolean isValidTagCount(int tagCount) {
        return tagCount <= MAX_TAG_COUNT;
    }

    /**
     * 태그 길이 검증
     * @param tag 태그
     * @return 유효 여부
     */
    public boolean isValidTagLength(String tag) {
        return tag != null && tag.length() <= MAX_TAG_LENGTH;
    }
}
