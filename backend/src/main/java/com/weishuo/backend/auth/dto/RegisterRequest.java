package com.weishuo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    String username;

    @NotBlank
    @Size(min = 6, max = 72)
    String password;

    @NotBlank
    @Size(max = 100)
    String displayName;

    @Email
    @Size(max = 120)
    String email;

    @Size(max = 255)
    String avatarUrl;
}
