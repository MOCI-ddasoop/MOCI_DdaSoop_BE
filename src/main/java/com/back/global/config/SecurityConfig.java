package com.back.global.config;

import com.back.global.security.JwtAuthenticationFilter;
import com.back.global.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/** Spring Security 설정 (JWT 인증, OAuth2 소셜 로그인, H2 콘솔 허용) */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private final ApplicationContext applicationContext;
    
    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverClientId;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        if (hasOAuth2ClientConfigured() && oAuth2AuthenticationSuccessHandler != null 
                && hasOAuth2Bean()) {
            http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2AuthenticationSuccessHandler)
            );
        }
        
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }

    /** OAuth2 클라이언트 설정 확인 */
    private boolean hasOAuth2ClientConfigured() {
        return StringUtils.hasText(googleClientId) 
                || StringUtils.hasText(kakaoClientId) 
                || StringUtils.hasText(naverClientId);
    }

    /** OAuth2 관련 빈 존재 여부 확인 */
    private boolean hasOAuth2Bean() {
        if (applicationContext == null) {
            return false;
        }
        try {
            return applicationContext.getBean(ClientRegistrationRepository.class) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
