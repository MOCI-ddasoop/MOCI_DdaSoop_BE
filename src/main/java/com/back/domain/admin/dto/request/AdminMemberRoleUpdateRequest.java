package com.back.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMemberRoleUpdateRequest {

    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "USER|ADMIN", message = "역할은 USER 또는 ADMIN만 가능합니다.")
    private String role;
}
