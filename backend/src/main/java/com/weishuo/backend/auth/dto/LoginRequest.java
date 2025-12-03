package com.weishuo.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    String username;

    @NotBlank
    @Size(min = 6, max = 72)
    String password;
}
