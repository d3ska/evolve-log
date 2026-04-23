package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.UserDto;
import com.deska.evolvelog.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@AuthenticationPrincipal Object principal) {
        if (principal instanceof User user) {
            return ResponseEntity.ok(ApiResponse.success(UserDto.from(user)));
        }
        if (principal instanceof OAuth2User oAuth2User) {
            String googleSub = oAuth2User.getAttribute("sub");
            User user = userRepository.findByGoogleSub(googleSub)
                    .orElseThrow(() -> new IllegalStateException("OAuth2 user not found in database"));
            return ResponseEntity.ok(ApiResponse.success(UserDto.from(user)));
        }
        return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
    }
}
