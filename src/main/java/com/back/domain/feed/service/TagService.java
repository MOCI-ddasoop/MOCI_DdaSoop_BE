package com.back.domain.feed.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 태그 처리 관련 서비스
 */
@Slf4j
@Service
public class TagService {

    // 해시태그 정규식: 한글, 영문, 숫자, 언더스코어 허용
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([가-힣a-zA-Z0-9_]+)");
    
    // 태그 최대 길이
    private static final int MAX_TAG_LENGTH = 50;
    
    // 태그 최대 개수
    private static final int MAX_TAG_COUNT = 30;

    /**
     * content에서 해시태그 추출
     * @param content 피드 내용
     * @return # 제외한 태그 목록
     */
    public List<String> extractHashtags(String content) {
        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        List<String> tags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);

        while (matcher.find()) {
            String tag = matcher.group(1);  // # 제외하고 추출
            
            // 태그 길이 검증
            if (tag.length() <= MAX_TAG_LENGTH) {
                tags.add(tag);
            } else {
                log.warn("태그가 너무 깁니다. 무시됨: {}", tag);
            }
        }

        return tags;
    }

    /**
     * 태그 정제 및 검증
     * @param requestTags 프론트에서 받은 태그 목록
     * @param content 피드 내용
     * @return 정제된 태그 목록
     */
    public List<String> refineTags(List<String> requestTags, String content) {
        List<String> refinedTags = new ArrayList<>();

        // 1. 프론트가 보낸 태그가 있으면 우선 사용
        if (requestTags != null && !requestTags.isEmpty()) {
            refinedTags = requestTags.stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(String::trim)
                    .filter(tag -> tag.length() <= MAX_TAG_LENGTH)
                    .distinct()  // 중복 제거
                    .limit(MAX_TAG_COUNT)  // 최대 개수 제한
                    .collect(Collectors.toList());
        }
        // 2. 프론트가 태그를 안 보냈으면, content에서 추출 해버리기 (백업 로직)
        else {
            refinedTags = extractHashtags(content).stream()
                    .distinct()
                    .limit(MAX_TAG_COUNT)
                    .collect(Collectors.toList());
        }

        log.debug("정제된 태그: {}", refinedTags);
        return refinedTags;
    }

    /**
     * content와 tags가 일치하는지 검증 (선택적 사용)
     * @param content 피드 내용
     * @param tags 프론트가 보낸 태그 목록
     * @return 일치 여부
     */
    public boolean validateTags(String content, List<String> tags) {
        List<String> actualTags = extractHashtags(content);
        
        // tags가 content에 실제로 있는 해시태그와 일치하는지 확인
        if (tags == null || tags.isEmpty()) {
            return actualTags.isEmpty();
        }

        // 순서는 상관없이, 모든 태그가 content에 있는지만 확인
        return actualTags.containsAll(tags);
    }

    /**
     * 태그 정규화 (소문자 변환, 공백 제거 등)
     * 필요시 사용
     */
    public String normalizeTag(String tag) {
        if (tag == null) {
            return null;
        }
        return tag.trim()
                .replaceAll("\\s+", "")  // 공백 제거
                .toLowerCase();  // 소문자 변환 (선택사항)
    }

    /**
     * 태그에서 특수문자 제거
     */
    public String sanitizeTag(String tag) {
        if (tag == null) {
            return null;
        }
        // # 제거 (만약 프론트에서 #포함해서 보낸 경우)
        String sanitized = tag.trim().replaceAll("^#+", "");
        
        // 한글, 영문, 숫자, 언더스코어만 허용
        sanitized = sanitized.replaceAll("[^가-힣a-zA-Z0-9_]", "");
        
        return sanitized;
    }
}
