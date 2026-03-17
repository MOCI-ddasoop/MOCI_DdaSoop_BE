package com.back.global.config;

import com.back.global.security.JwtAuthenticationFilter;
import com.back.global.security.OAuth2AuthenticationSuccessHandler;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/** Spring Security 설정 (JWT 인증, OAuth2 소셜 로그인, H2 콘솔 허용) */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
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
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        
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
                .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/*").permitAll()
                .requestMatchers("/_next/static/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs").permitAll()
                .requestMatchers("/v3/api-docs/*").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/feeds",
                        "/api/feeds/*",
                        "/api/feeds/members/*/scroll",
                        "/api/feeds/together/*/scroll",
                        "/api/feeds/search/tag",
                        "/api/feeds/bookmarks/members/*",
                        "/api/feeds/together/*/notices",
                        "/api/feeds/together/*/notices/pinned"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/together/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/donation/list",
                        "/api/v1/donation/list/*",
                        "/api/v1/donation/list/*/description",
                        "/api/v1/donation/list/*/donorList",
                        "/api/v1/donation/notice/list",
                        "/api/v1/donation/notice/*",
                        "/api/v1/donation/payment/recent"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // API 인증 실패는 로그인 페이지 리다이렉트 대신 401 응답 처리
            .exceptionHandling(exception -> exception
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new RegexRequestMatcher("^/api/.*", null)
                )
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }

    /** CORS 설정 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
            "https://www.ddasoop.xyz",
            "https://ddasoop.xyz",
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        ));

        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type"
        ));

        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Set-Cookie"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
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
