package com.quizapp.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The caller's IP, for rate limiting. Render sits behind Cloudflare and a reverse proxy that APPENDS to
 * X-Forwarded-For without clearing what the client sent - so its first entry is whatever the client typed, and
 * reading that one let anyone dodge every per-IP limit by sending a new value each time. In order of trust:
 *   1. True-Client-IP - set by Cloudflare at Render's edge from the real connection;
 *   2. the LAST X-Forwarded-For entry - the one Render's own proxy appended;
 *   3. the socket address (local development).
 * Still only an IP: limits that guard a password (logins) also count per account, which no header can dodge.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String trueClient = request.getHeader("True-Client-IP");
        if (trueClient != null && !trueClient.isBlank()) {
            return trueClient.trim();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            String last = hops[hops.length - 1].trim();
            if (!last.isEmpty()) return last;
        }
        return request.getRemoteAddr();
    }
}
