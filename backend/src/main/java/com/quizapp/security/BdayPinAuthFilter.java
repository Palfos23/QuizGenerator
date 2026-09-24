package com.quizapp.security;

import com.quizapp.exception.TooManyAttemptsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// A tiny, standalone gate for the one-off birthday-quiz feature (see
// BdayController) - deliberately NOT part of the real admin/JWT/role system,
// so a party guest never ends up holding a credential that also opens the
// full admin panel. Every /api/bday/** request must carry the configured PIN
// in X-Bday-Pin; every other request passes through untouched (this filter
// is registered globally in SecurityConfig, so it has to self-scope).
public class BdayPinAuthFilter extends OncePerRequestFilter {

    private final String expectedPin;
    private final LoginRateLimiter rateLimiter;

    public BdayPinAuthFilter(String expectedPin, LoginRateLimiter rateLimiter) {
        this.expectedPin = expectedPin;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/bday/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientKey = clientKey(request);
        // This filter runs before Spring MVC dispatch, so GlobalExceptionHandler
        // never sees it - has to write its own error response for a lockout,
        // same as the wrong-PIN case below.
        try {
            rateLimiter.checkAllowed(clientKey);
        } catch (TooManyAttemptsException e) {
            writeJson(response, 429, e.getMessage()); // HttpServletResponse has no SC_TOO_MANY_REQUESTS constant
            return;
        }

        String providedPin = request.getHeader("X-Bday-Pin");
        if (expectedPin == null || expectedPin.isBlank() || !expectedPin.equals(providedPin)) {
            rateLimiter.recordFailure(clientKey);
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Wrong PIN.");
            return;
        }

        rateLimiter.recordSuccess(clientKey);
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "bday:" + forwardedFor.split(",")[0].trim();
        }
        return "bday:" + request.getRemoteAddr();
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "'") + "\"}");
    }
}
