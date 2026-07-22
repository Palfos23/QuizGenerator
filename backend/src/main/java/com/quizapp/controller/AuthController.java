package com.quizapp.controller;

import com.quizapp.dto.AdminLoginRequest;
import com.quizapp.dto.AuthResponse;
import com.quizapp.dto.GoogleLoginRequest;
import com.quizapp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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

    /** Admins: username/password login - a completely separate credential store from AppUser. Rate-limited per IP. */
    @PostMapping("/admin/login")
    public AuthResponse adminLogin(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        return authService.loginAsAdmin(request.getUsername(), request.getPassword(), clientKey(httpRequest));
    }

    /**
     * Render (like most PaaS hosts) sits behind a reverse proxy, so the real client IP is in
     * X-Forwarded-For, not getRemoteAddr(). Falls back to getRemoteAddr() for local dev.
     */
    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
