package com.quizapp.security;

import com.quizapp.exception.TooManyAttemptsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// A second lock on the game master's side of the advent calendar: on top of the admin login, every
// /api/admin/advent/** request must carry the calendar's password in X-Advent-Password. The page behind it shows the
// hidden answers, so being an admin alone doesn't open it.
//
// Wrong password = 403 (not 401: the frontend logs a person out on any 401). Only requests that are already signed in
// are checked - an anonymous request falls through to the normal 401 - so the password can't be guessed from outside, and
// repeated wrong guesses lock the client out for a while (the same limiter the logins use).
public class AdventPasswordFilter extends OncePerRequestFilter {

    static final String PREFIX = "/api/admin/advent";

    private final String expectedPassword;
    private final LoginRateLimiter rateLimiter;

    public AdventPasswordFilter(String expectedPassword, LoginRateLimiter rateLimiter) {
        this.expectedPassword = expectedPassword;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // A CORS preflight carries no custom headers by design - nothing to guard.
        if (!request.getRequestURI().startsWith(PREFIX) || "OPTIONS".equalsIgnoreCase(request.getMethod()) || !signedIn()) {
            chain.doFilter(request, response);
            return;
        }

        String clientKey = clientKey(request);
        try {
            rateLimiter.checkAllowed(clientKey);
        } catch (TooManyAttemptsException e) {
            writeJson(response, 429, e.getMessage()); // HttpServletResponse has no SC_TOO_MANY_REQUESTS constant
            return;
        }

        if (!matches(request.getHeader("X-Advent-Password"))) {
            rateLimiter.recordFailure(clientKey);
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, "Feil passord.");
            return;
        }
        rateLimiter.recordSuccess(clientKey);
        chain.doFilter(request, response);
    }

    private static boolean signedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** An empty configured password locks the page for everyone; the comparison doesn't leak where the texts differ. */
    private boolean matches(String provided) {
        if (expectedPassword == null || expectedPassword.isBlank() || provided == null) return false;
        return MessageDigest.isEqual(expectedPassword.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "advent:" + forwardedFor.split(",")[0].trim();
        }
        return "advent:" + request.getRemoteAddr();
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "'") + "\"}");
    }
}
