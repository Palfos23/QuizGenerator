package com.quizapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${quiz.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // login endpoints, and dev-only H2 console, are open
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // anyone can see which categories exist before logging in (nice for a landing page)
                        .requestMatchers("/api/quiz/categories").permitAll()
                        // admin question-bank management: ADMIN role only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // online multiplayer rooms (create/join/play): USER, ADMIN, or a
                        // no-account GUEST token from POST /api/auth/guest - GUEST is
                        // deliberately scoped to just this one path prefix (see
                        // RoomController's own guest-vs-host rules for the finer-grained
                        // "guests may join but never host" split within it) - every other
                        // route below requires a real account, GUEST included nowhere else.
                        .requestMatchers("/api/rooms/**").authenticated()
                        // generating/exporting quizzes: any logged-in user (USER or ADMIN)
                        .requestMatchers("/api/quiz/**").hasAnyRole("USER", "ADMIN")
                        // browsing/playing weekly grids: any logged-in user (USER or ADMIN)
                        .requestMatchers("/api/grids/**").hasAnyRole("USER", "ADMIN")
                        // browsing/playing Starting XI boards: any logged-in user (USER or ADMIN)
                        .requestMatchers("/api/lineups/**").hasAnyRole("USER", "ADMIN")
                        // fetching tension questions/autocomplete: any logged-in user (USER or ADMIN)
                        .requestMatchers("/api/tension/**").hasAnyRole("USER", "ADMIN")
                        // browsing/copying admin-published quiz templates: any logged-in user
                        .requestMatchers("/api/quiz-templates/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/501/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/bullseye/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/penalty-shootouts/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/flashback/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/play-access/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().hasAnyRole("USER", "ADMIN"))
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // needed for the H2 console
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
