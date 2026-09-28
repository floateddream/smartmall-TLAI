package com.smartmall.tlai.web.auth;

import com.smartmall.tlai.common.api.ApiResponse;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.security.auth.AuthService;
import com.smartmall.tlai.security.auth.LoginCommand;
import com.smartmall.tlai.security.auth.RegisterCommand;
import com.smartmall.tlai.web.auth.dto.AuthResponse;
import com.smartmall.tlai.web.auth.dto.LoginRequest;
import com.smartmall.tlai.web.auth.dto.RegisterRequest;
import com.smartmall.tlai.web.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        var user = authService.register(new RegisterCommand(request.username(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(UserResponse.from(user)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(AuthResponse.from(
                authService.login(new LoginCommand(request.account(), request.password()))));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.success(UserResponse.from(authService.currentUser(principal)));
    }
}
