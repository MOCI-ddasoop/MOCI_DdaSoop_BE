package com.back.domain.together.controller;

import com.back.domain.together.dto.response.TogetherResponse;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherStatus;
import com.back.domain.together.service.TogetherService;
import com.back.global.jwt.JwtTokenProvider;
import com.back.global.security.JwtAuthenticationFilter;
import com.back.global.security.OAuth2AuthenticationSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TogetherController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
})
@ActiveProfiles("test")
public class TogetherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TogetherService togetherService;

    @Test
    @DisplayName("1. 함께하기 게시글 등록")
    void createTogether_success() throws Exception {

        String requestJson = """
    {
      "title": "주말 플로깅 같이 해요",
      "description": "한강에서 플로깅 하실 분 모집합니다",
      "category": "PLOGGING",
      "mode": "OFFLINE",
      "capacity": 10,
      "startDate": "2025-01-20",
      "endDate": "2025-01-20"
    }
    """;

        TogetherResponse response = TogetherResponse.builder()
                .id(1L)
                .title("주말 플로깅 같이 해요")
                .description("한강에서 플로깅 하실 분 모집합니다")
                .category(TogetherCategory.PLOGGING)
                .mode(TogetherMode.OFFLINE)
                .capacity(10)
                .status(TogetherStatus.RECRUITING)
                .organizerId(1L)
                .build();

        Mockito.when(togetherService.create(Mockito.any(), Mockito.anyLong()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/together")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1L));
    }
}
