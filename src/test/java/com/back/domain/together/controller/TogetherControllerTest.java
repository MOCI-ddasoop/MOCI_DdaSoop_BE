package com.back.domain.together.controller;

import com.back.domain.together.dto.request.TogetherRequest;
import com.back.domain.together.dto.response.TogetherResponse;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherStatus;
import com.back.domain.together.service.TogetherService;
import com.back.global.rsData.RsData;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(TogetherController.class)
public class TogetherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TogetherService togetherService;

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("1. 함께하기 게시글 등록")
    void createTogether_success() throws Exception {

        String requestJson = """
    {
      "title": "주말 플로깅 같이 해요",
      "description": "한강에서 플로깅 하실 분 모집합니다",
      "category": "category1",
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
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1L));
    }
}
