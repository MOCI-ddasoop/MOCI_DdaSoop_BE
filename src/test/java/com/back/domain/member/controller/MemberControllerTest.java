package com.back.domain.member.controller;

import com.back.domain.member.dto.request.EmailCheckRequest;
import com.back.domain.member.dto.request.MemberUpdateRequest;
import com.back.domain.member.dto.request.MemberWithdrawRequest;
import com.back.domain.member.dto.request.NicknameCheckRequest;
import com.back.domain.member.dto.response.MemberInfoResponse;
import com.back.domain.member.service.MemberService;
import com.back.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MemberController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("removal")
    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("1. 내 정보 조회 성공")
    void getMyInfo_success() throws Exception {
        // given
        Long memberId = 1L;
        MemberInfoResponse response = MemberInfoResponse.builder()
                .memberId(1L)
                .name("홍길동")
                .nickname("hong123")
                .email("hong@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .role("USER")
                .lastLoginProvider("KAKAO")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(memberService.getMemberInfo(memberId))
                .thenReturn(response);

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        get("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.nickname").value("hong123"))
                .andExpect(jsonPath("$.email").value("hong@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.lastLoginProvider").value("KAKAO"));
    }

    @Test
    @DisplayName("2. 내 정보 조회 실패 - 회원을 찾을 수 없음")
    void getMyInfo_fail_memberNotFound() throws Exception {
        // given
        Long memberId = 1L;

        Mockito.when(memberService.getMemberInfo(memberId))
                .thenThrow(new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        get("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("3. 닉네임 중복 체크 - 사용 가능")
    void checkNickname_available() throws Exception {
        // given
        NicknameCheckRequest request = NicknameCheckRequest.builder()
                .nickname("newNickname")
                .build();

        Mockito.when(memberService.checkNickname("newNickname"))
                .thenReturn(false);

        // when & then
        mockMvc.perform(
                        post("/api/members/check-nickname")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 닉네임입니다."));
    }

    @Test
    @DisplayName("4. 닉네임 중복 체크 - 사용 불가")
    void checkNickname_unavailable() throws Exception {
        // given
        NicknameCheckRequest request = NicknameCheckRequest.builder()
                .nickname("existing")
                .build();

        Mockito.when(memberService.checkNickname("existing"))
                .thenReturn(true);

        // when & then
        mockMvc.perform(
                        post("/api/members/check-nickname")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("5. 닉네임 중복 체크 실패 - 유효성 검증 실패 (빈 값)")
    void checkNickname_fail_validation_empty() throws Exception {
        // given
        NicknameCheckRequest request = NicknameCheckRequest.builder()
                .nickname("")
                .build();

        // when & then
        mockMvc.perform(
                        post("/api/members/check-nickname")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. 닉네임 중복 체크 실패 - 유효성 검증 실패 (길이 초과)")
    void checkNickname_fail_validation_tooLong() throws Exception {
        // given
        NicknameCheckRequest request = NicknameCheckRequest.builder()
                .nickname("thisIsTooLongNickname")
                .build();

        // when & then
        mockMvc.perform(
                        post("/api/members/check-nickname")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. 이메일 중복 체크 - 사용 가능")
    void checkEmail_available() throws Exception {
        // given
        EmailCheckRequest request = EmailCheckRequest.builder()
                .email("newemail@example.com")
                .build();

        Mockito.when(memberService.checkEmail("newemail@example.com"))
                .thenReturn(false);

        // when & then
        mockMvc.perform(
                        post("/api/members/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 이메일입니다."));
    }

    @Test
    @DisplayName("8. 이메일 중복 체크 - 사용 불가")
    void checkEmail_unavailable() throws Exception {
        // given
        EmailCheckRequest request = EmailCheckRequest.builder()
                .email("existing@example.com")
                .build();

        Mockito.when(memberService.checkEmail("existing@example.com"))
                .thenReturn(true);

        // when & then
        mockMvc.perform(
                        post("/api/members/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("9. 이메일 중복 체크 실패 - 유효성 검증 실패 (잘못된 형식)")
    void checkEmail_fail_validation_invalidFormat() throws Exception {
        // given
        EmailCheckRequest request = EmailCheckRequest.builder()
                .email("invalid-email")
                .build();

        // when & then
        mockMvc.perform(
                        post("/api/members/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("10. 회원 정보 수정 성공")
    void updateMember_success() throws Exception {
        // given
        Long memberId = 1L;
        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .email("newemail@example.com")
                .nickname("newNickname")
                .profileImageUrl("https://example.com/new-profile.jpg")
                .build();

        Mockito.doNothing()
                .when(memberService)
                .updateMember(memberId, request);

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        put("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("11. 회원 정보 수정 실패 - 유효성 검증 실패 (잘못된 이메일 형식)")
    void updateMember_fail_validation_invalidEmail() throws Exception {
        // given
        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .email("invalid-email")
                .nickname("newNickname")
                .build();

        // SecurityContext 설정
        Long memberId = 1L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        put("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("12. 회원 정보 수정 실패 - 중복된 이메일")
    void updateMember_fail_duplicateEmail() throws Exception {
        // given
        Long memberId = 1L;
        MemberUpdateRequest request = MemberUpdateRequest.builder()
                .email("duplicate@example.com")
                .nickname("newNickname")
                .build();

        Mockito.doThrow(new IllegalArgumentException("이미 사용 중인 이메일입니다."))
                .when(memberService)
                .updateMember(Mockito.eq(1L), Mockito.any(MemberUpdateRequest.class));

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        put("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("13. 회원 탈퇴 성공")
    void withdrawMember_success() throws Exception {
        // given
        Long memberId = 1L;
        MemberWithdrawRequest request = MemberWithdrawRequest.builder()
                .reason("개인적인 사유로 인한 탈퇴")
                .build();

        Mockito.doNothing()
                .when(memberService)
                .withdrawMember(memberId, request.getReason());

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        delete("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    @DisplayName("14. 회원 탈퇴 성공 - 사유 없이")
    void withdrawMember_success_withoutReason() throws Exception {
        // given
        Long memberId = 1L;

        Mockito.doNothing()
                .when(memberService)
                .withdrawMember(memberId, null);

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        delete("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));
    }

    @Test
    @DisplayName("15. 회원 탈퇴 실패 - 회원을 찾을 수 없음")
    void withdrawMember_fail_memberNotFound() throws Exception {
        // given
        Long memberId = 1L;  // MemberController에서 하드코딩된 값과 일치해야 함
        MemberWithdrawRequest request = MemberWithdrawRequest.builder()
                .reason("탈퇴 사유")
                .build();

        Mockito.doThrow(new IllegalArgumentException("회원을 찾을 수 없습니다."))
                .when(memberService)
                .withdrawMember(Mockito.eq(memberId), Mockito.anyString());

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberId, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // when & then
        mockMvc.perform(
                        delete("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."));
    }
}

