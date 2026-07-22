package com.quizapp.service;

import com.quizapp.dto.AuthResponse;
import com.quizapp.model.AdminUser;
import com.quizapp.model.AppUser;
import com.quizapp.repository.AdminUserRepository;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.security.GoogleTokenVerifier;
import com.quizapp.security.JwtService;
import com.quizapp.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final AdminUserRepository adminUserRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(AppUserRepository appUserRepository,
                        AdminUserRepository adminUserRepository,
                        GoogleTokenVerifier googleTokenVerifier,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        LoginRateLimiter loginRateLimiter) {
        this.appUserRepository = appUserRepository;
        this.adminUserRepository = adminUserRepository;
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleTokenVerifier.VerifiedGoogleUser verified = googleTokenVerifier.verify(idToken);
        if (verified == null) {
            throw new IllegalArgumentException("Google sign-in could not be verified. Please try again.");
        }

        AppUser user = appUserRepository.findByEmail(verified.email())
                .orElseGet(() -> {
                    AppUser fresh = new AppUser();
                    fresh.setEmail(verified.email());
                    fresh.setName(verified.name());
                    fresh.setGoogleSubject(verified.subject());
                    return appUserRepository.save(fresh);
                });

        String token = jwtService.generateToken(user.getEmail(), "USER", user.getId(), user.getName());
        return new AuthResponse(token, user.getName(), "USER");
    }

    /** clientKey identifies the caller for rate-limiting purposes - the request's IP address. */
    public AuthResponse loginAsAdmin(String username, String rawPassword, String clientKey) {
        loginRateLimiter.checkAllowed(clientKey);

        AdminUser admin = adminUserRepository.findByUsername(username).orElse(null);

        if (admin == null || !passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
            loginRateLimiter.recordFailure(clientKey);
            throw new IllegalArgumentException("Invalid admin credentials.");
        }

        loginRateLimiter.recordSuccess(clientKey);
        String token = jwtService.generateToken(admin.getUsername(), "ADMIN", admin.getId(), admin.getUsername());
        return new AuthResponse(token, admin.getUsername(), "ADMIN");
    }
}
