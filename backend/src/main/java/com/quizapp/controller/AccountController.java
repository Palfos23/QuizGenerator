package com.quizapp.controller;

import com.quizapp.dto.AccountExportDto;
import com.quizapp.dto.DeleteAccountRequest;
import com.quizapp.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Self-service GDPR rights for a signed-in AppUser - falls under the existing
// anyRequest().hasAnyRole("USER", "ADMIN") catch-all in SecurityConfig (no
// dedicated matcher needed), which already excludes GUEST - a guest has no
// AppUser row to act on anyway.
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/export")
    public AccountExportDto export(Authentication authentication) {
        return accountService.exportData(authentication.getName());
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@RequestBody(required = false) DeleteAccountRequest request,
                                        Authentication authentication) {
        String password = request != null ? request.getPassword() : null;
        accountService.deleteAccount(authentication.getName(), password);
        return ResponseEntity.noContent().build();
    }
}
