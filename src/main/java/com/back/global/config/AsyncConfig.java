package com.back.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 활성화 Configuration
 * 
 * @Async 어노테이션을 사용한 비동기 메서드 실행을 활성화합니다.
 * MemberTagStatisticsService의 updateStatisticsAsync()가 비동기로 실행됩니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // 기본 ThreadPoolTaskExecutor 사용
    // 별도 설정이 필요한 경우 @Bean으로 Executor 생성 가능
}
