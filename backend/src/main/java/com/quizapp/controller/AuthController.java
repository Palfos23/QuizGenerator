package com.quizapp.controller;

import com.quizapp.dto.AdminLoginRequest;
import com.quizapp.dto.AuthResponse;
import com.quizapp.dto.GoogleLoginRequest;
import com.quizapp.security.JwtService;
import com.quizapp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
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
     * Silently swaps a still-valid token for a fresh one with a full new expiry -
     * called periodically by App.vue's checkSessionTimers while a tab is open and
     * the user hasn't been idle long enough to trip the separate inactivity
     * logout. Sits under /api/auth/**, so it's permitAll() in SecurityConfig same
     * as the two logins above; the token itself (validated here, not by the usual
     * ROLE-gated route rule) is what proves who's asking. An expired/malformed
     * token makes parseClaims() throw JwtException, which GlobalExceptionHandler
     * maps to 401 - the frontend then falls back to its normal "session expired"
     * flow rather than looping on a token that's already unrecoverable.
     */
    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        // required = false, checked manually below - a bare @RequestHeader (required
        // defaults to true) makes Spring reject a request with no such header before
        // this method body ever runs, as a raw 500 (MissingRequestHeaderException
        // isn't one GlobalExceptionHandler maps to anything friendlier), not the 400
        // this check is meant to produce.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token.");
        }
        String oldToken = authorizationHeader.substring(7);
        JwtService.RefreshResult result = jwtService.refresh(oldToken);
        return new AuthResponse(result.token(), result.displayName(), result.role());
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
