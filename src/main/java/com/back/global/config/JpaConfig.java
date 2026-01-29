package com.back.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * 
 * @CreatedDate, @LastModifiedDate 자동 설정을 위한 Configuration
 * BaseEntity를 상속받는 모든 엔티티에 자동으로 생성/수정 시간이 기록됩니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
