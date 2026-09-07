package com.quizapp.service;

import com.quizapp.dto.AuthResponse;
import com.quizapp.model.AdminUser;
import com.quizapp.model.AppUser;
import com.quizapp.repository.AdminUserRepository;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.security.GoogleTokenVerifier;
import com.quizapp.security.JwtService;
import com.quizapp.security.LoginRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final AppUserRepository appUserRepository;
    private final AdminUserRepository adminUserRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;
    private final MailService mailService;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository appUserRepository,
                        AdminUserRepository adminUserRepository,
                        GoogleTokenVerifier googleTokenVerifier,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        LoginRateLimiter loginRateLimiter,
                        MailService mailService,
                        @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.appUserRepository = appUserRepository;
        this.adminUserRepository = adminUserRepository;
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
        this.mailService = mailService;
        this.frontendBaseUrl = frontendBaseUrl;
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

    /**
     * Regular users who don't have (or don't want to use) a Google account. Lives
     * alongside loginWithGoogle rather than replacing it - either method works for
     * an AppUser row that ends up with both a googleSubject and a passwordHash set.
     */
    @Transactional
    public AuthResponse registerWithPassword(String rawEmail, String rawPassword, String name) {
        String email = normalizeEmail(rawEmail);
        AppUser existing = appUserRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getPasswordHash() == null) {
                throw new IllegalArgumentException(
                        "That email already has an account signed in with Google - use \"Continue with Google\" instead.");
            }
            throw new IllegalArgumentException("An account with that email already exists - try signing in instead.");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        appUserRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), "USER", user.getId(), user.getName());
        return new AuthResponse(token, user.getName(), "USER");
    }

    /** clientKey identifies the caller for rate-limiting purposes - the request's IP address. */
    public AuthResponse loginWithPassword(String rawEmail, String rawPassword, String clientKey) {
        String key = "user-login:" + clientKey;
        loginRateLimiter.checkAllowed(key);

        String email = normalizeEmail(rawEmail);
        AppUser user = appUserRepository.findByEmail(email).orElse(null);

        if (user == null || user.getPasswordHash() == null) {
            loginRateLimiter.recordFailure(key);
            // Same generic message whether the account doesn't exist or is Google-only -
            // a distinct message for "this is a Google account" would let this endpoint be
            // used to probe which emails have accounts at all.
            throw new IllegalArgumentException("Invalid email or password.");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginRateLimiter.recordFailure(key);
            throw new IllegalArgumentException("Invalid email or password.");
        }

        loginRateLimiter.recordSuccess(key);
        String token = jwtService.generateToken(user.getEmail(), "USER", user.getId(), user.getName());
        return new AuthResponse(token, user.getName(), "USER");
    }

    /**
     * Always looks like it succeeded from the caller's point of view, regardless of
     * whether the email belongs to an account, a Google-only account, or nothing at
     * all - otherwise this endpoint could be used to check which emails are
     * registered. The actual email only goes out for a real password-having account.
     */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        AppUser user = appUserRepository.findByEmail(email).orElse(null);
        if (user == null || user.getPasswordHash() == null) {
            return;
        }

        byte[] rawTokenBytes = new byte[32];
        secureRandom.nextBytes(rawTokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawTokenBytes);

        user.setResetTokenHash(sha256Hex(rawToken));
        user.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
        appUserRepository.save(user);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
        mailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        AppUser user = appUserRepository.findByResetTokenHash(sha256Hex(rawToken)).orElse(null);
        if (user == null || user.getResetTokenExpiresAt() == null
                || Instant.now().isAfter(user.getResetTokenExpiresAt())) {
            throw new IllegalArgumentException("This reset link is invalid or has expired - request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetTokenHash(null);
        user.setResetTokenExpiresAt(null);
        appUserRepository.save(user);
    }

    /**
     * No AppUser row is created at all - a guest is just a JWT good for joining an
     * existing room (see PlayAccessService and RoomController's guest-specific
     * rules), with a synthetic, never-reused subject so two guests who happen to
     * type the same display name still get distinct room-participant identities.
     * Nothing to clean up afterwards since nothing was ever persisted.
     */
    public AuthResponse loginAsGuest(String displayName) {
        String trimmedName = displayName.trim();
        String subject = "guest:" + UUID.randomUUID();
        String token = jwtService.generateToken(subject, "GUEST", null, trimmedName);
        return new AuthResponse(token, trimmedName, "GUEST");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JCA algorithm on every standard JVM - this can't happen.
            throw new IllegalStateException(e);
        }
    }
}
