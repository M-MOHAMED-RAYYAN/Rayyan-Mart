package com.shopping.system.controller;

import com.shopping.system.dto.ApiResponse;
import com.shopping.system.dto.LoginRequest;
import com.shopping.system.dto.LoginResponse;
import com.shopping.system.dto.RegisterRequest;
import com.shopping.system.dto.UserResponse;
import com.shopping.system.service.AuthService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ApiResponse.success("Registration successful", user);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return authService.login(request, session);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse logout(HttpSession session) {
        authService.logout(session);
        return ApiResponse.success("Logged out successfully", null);
    }

    @GetMapping("/me")
    public UserResponse me(HttpSession session) {
        return authService.me(session);
    }
}