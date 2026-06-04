package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.UpdateLocaleRequest;
import com.deska.evolvelog.dto.request.UpdateUserPreferencesRequest;
import com.deska.evolvelog.dto.response.UserDto;
import com.deska.evolvelog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/preferences")
    public ResponseEntity<ApiResponse<UserDto>> updatePreferences(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        User updated = userService.updatePreferences(user, request);
        return ResponseEntity.ok(ApiResponse.success(UserDto.from(updated)));
    }

    @PutMapping("/me/locale")
    public ResponseEntity<ApiResponse<UserDto>> updateLocale(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateLocaleRequest request
    ) {
        User updated = userService.updateLocale(user, request.locale());
        return ResponseEntity.ok(ApiResponse.success(UserDto.from(updated)));
    }
}
