package com.weishuo.backend.auth.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {

    Long id;
    String username;
    String displayName;
    String avatarUrl;
    String email;
    Instant createdAt;
    String message;
}
