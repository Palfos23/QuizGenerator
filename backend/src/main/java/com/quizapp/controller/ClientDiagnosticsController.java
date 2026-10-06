package com.quizapp.controller;

import com.quizapp.dto.ClientEventRequest;
import com.quizapp.service.ClientEventLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diagnostics")
public class ClientDiagnosticsController {

    private final ClientEventLogService clientEventLogService;

    public ClientDiagnosticsController(ClientEventLogService clientEventLogService) {
        this.clientEventLogService = clientEventLogService;
    }

    /** Always 204 - the browser fires this and forgets; a dropped (over-cap) event isn't an error to it. */
    @PostMapping("/client-event")
    public ResponseEntity<Void> clientEvent(@Valid @RequestBody ClientEventRequest request,
                                            Authentication authentication,
                                            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("");
        clientEventLogService.record(authentication.getName(), role, request, userAgent);
        return ResponseEntity.noContent().build();
    }
}
