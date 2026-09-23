package com.quizapp.security;

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

    public BdayPinAuthFilter(String expectedPin) {
        this.expectedPin = expectedPin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/bday/")) {
            chain.doFilter(request, response);
            return;
        }

        String providedPin = request.getHeader("X-Bday-Pin");
        if (expectedPin == null || expectedPin.isBlank() || !expectedPin.equals(providedPin)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Wrong PIN.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
