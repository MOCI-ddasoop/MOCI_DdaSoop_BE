package com.back.domain.member.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

package com.back.domain.member.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String MEMBER_TOKENS_KEY_PREFIX = "member:";
    private static final String MEMBER_TOKENS_KEY_SUFFIX = ":tokens";
    private static final int DEFAULT_TTL_SECONDS = 7 * 24 * 60 * 60;

    public void save(String tokenHash, Long memberId, int ttlSeconds) {
        String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
        String memberTokensKey = MEMBER_TOKENS_KEY_PREFIX + memberId + MEMBER_TOKENS_KEY_SUFFIX;
        
        redisTemplate.opsForValue().set(tokenKey, memberId.toString(), Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForSet().add(memberTokensKey, tokenHash);
        redisTemplate.expire(memberTokensKey, Duration.ofSeconds(ttlSeconds));
    }

    public void save(String tokenHash, Long memberId) {
        save(tokenHash, memberId, DEFAULT_TTL_SECONDS);
    }

    public Optional<Long> findByTokenHash(String tokenHash) {
        String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
        String memberIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (memberIdStr == null) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(Long.parseLong(memberIdStr));
        } catch (NumberFormatException e) {
            log.error("회원 ID 파싱 실패 - memberIdStr: {}", memberIdStr, e);
            return Optional.empty();
        }
    }

    public void deleteByMemberId(Long memberId) {
        String memberTokensKey = MEMBER_TOKENS_KEY_PREFIX + memberId + MEMBER_TOKENS_KEY_SUFFIX;
        Set<String> tokenHashes = redisTemplate.opsForSet().members(memberTokensKey);
        
        if (tokenHashes != null && !tokenHashes.isEmpty()) {
            for (String tokenHash : tokenHashes) {
                redisTemplate.delete(TOKEN_KEY_PREFIX + tokenHash);
            }
        }
        
        redisTemplate.delete(memberTokensKey);
    }

    public void deleteByTokenHash(String tokenHash) {
        String tokenKey = TOKEN_KEY_PREFIX + tokenHash;
        String memberIdStr = redisTemplate.opsForValue().get(tokenKey);
        
        if (memberIdStr != null) {
            try {
                Long memberId = Long.parseLong(memberIdStr);
                redisTemplate.delete(tokenKey);
                String memberTokensKey = MEMBER_TOKENS_KEY_PREFIX + memberId + MEMBER_TOKENS_KEY_SUFFIX;
                redisTemplate.opsForSet().remove(memberTokensKey, tokenHash);
            } catch (NumberFormatException e) {
                log.error("회원 ID 파싱 실패 - memberIdStr: {}", memberIdStr, e);
            }
        }
    }

    public boolean existsByTokenHash(String tokenHash) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_KEY_PREFIX + tokenHash));
    }
}
