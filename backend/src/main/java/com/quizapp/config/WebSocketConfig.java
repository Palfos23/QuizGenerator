package com.quizapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Push-only channel for online room state - see RoomBroadcastService, which is
 * the only thing that ever sends on it. There's nothing for a client to
 * publish here (every actual game action still goes through the existing
 * REST endpoints, unchanged - this just replaces how OTHER players in the
 * room find out something happened, instead of each one polling for it every
 * couple of seconds).
 *
 * No STOMP-level auth on the handshake: a room's state is already only as
 * secret as its code (RoomController#get has never checked that the caller
 * is actually a participant either, matching this app's whole "the code is
 * the access control" model for a local party game), so a broadcast a client
 * can only reach by already knowing that same room code isn't a new
 * exposure. Every endpoint that can actually change something is still
 * behind the normal JWT-protected REST API, completely untouched by this.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${quiz.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker, fine for this app's whole deployment story (a single
        // Render instance) - same caveat LoginRateLimiter's own comment already
        // documents for its in-memory state: this would need a real broker
        // (RabbitMQ/ActiveMQ) to coordinate correctly across multiple instances.
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS wraps the connection with automatic fallback transports
        // (long-polling, etc.) for whatever a proxy in front of the app (Render's
        // included) doesn't pass a raw WebSocket upgrade through cleanly - more
        // robust than a bare WebSocket endpoint for exactly that reason.
        //
        // setSessionCookieNeeded(false): SockJS defaults to wanting a session-
        // affinity cookie (for sticky routing across multiple server instances) -
        // this app is already documented as single-instance only (see this
        // class's broker comment above), so there's no sticky-session need to
        // actually give up here.
        //
        // setSuppressCors(true): sockjs-client's own XHR fallback transports set
        // withCredentials=true unconditionally on every cross-origin request
        // they make, regardless of the line above - a sockjs-client default, not
        // something this server controls. A credentialed request needs
        // Access-Control-Allow-Credentials: true in the response or the browser
        // rejects the whole handshake outright (net::ERR_FAILED on /ws/info).
        // Spring's own built-in SockJS CORS handling (the default here) only
        // ever echoes the allowed origin, never that credentials header - this
        // hands CORS for /ws/** to Spring Security's filter instead, where
        // SecurityConfig now allows credentials (see its own comment on why
        // that's fine to do for the whole API here, not just this one path).
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins).withSockJS()
                .setSessionCookieNeeded(false)
                .setSuppressCors(true);
    }
}
