package com.quizapp.controller;

import com.quizapp.dto.AdminLoginRequest;
import com.quizapp.dto.AuthResponse;
import com.quizapp.dto.GoogleLoginRequest;
import com.quizapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Regular users: sign in with Google. The frontend gets an ID token from Google and sends it here. */
    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request.getIdToken());
    }

    /** Admins: username/password login - a completely separate credential store from AppUser. */
    @PostMapping("/admin/login")
    public AuthResponse adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return authService.loginAsAdmin(request.getUsername(), request.getPassword());
    }
}
