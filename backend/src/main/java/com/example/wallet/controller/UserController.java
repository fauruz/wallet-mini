package com.example.wallet;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Map.of("id", user.getId(), "email", user.getEmail());
    }
}
