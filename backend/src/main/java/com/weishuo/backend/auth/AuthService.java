package com.weishuo.backend.auth;

import com.weishuo.backend.auth.dto.AuthResponse;
import com.weishuo.backend.auth.dto.LoginRequest;
import com.weishuo.backend.auth.dto.RegisterRequest;
import com.weishuo.backend.user.User;
import com.weishuo.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已被占用");
        }

        User user = User.builder()
            .username(request.getUsername().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName().trim())
            .email(trimOrNull(request.getEmail()))
            .avatarUrl(trimOrNull(request.getAvatarUrl()))
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved, "注册成功");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        return toResponse(user, "登录成功");
    }

    private AuthResponse toResponse(User user, String message) {
        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .message(message)
                .build();
    }

    private String trimOrNull(String input) {
        return input == null ? null : input.trim();
    }
}
