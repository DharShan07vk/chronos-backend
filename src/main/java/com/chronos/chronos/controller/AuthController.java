package com.chronos.chronos.controller;


import com.chronos.chronos.dto.ApiResponse;
import com.chronos.chronos.dto.LoginRequest;
import com.chronos.chronos.dto.LoginResponse;
import com.chronos.chronos.dto.RegisterRequest;
import com.chronos.chronos.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("auth/")
public class AuthController {

    private final AuthService authService;

    @PostMapping("login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login Successful", response));
    }

    @PostMapping("register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Register Successful", response));
    }
}
