package com.careerflow.controller;
import com.careerflow.security.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
@RestController @RequiredArgsConstructor
public class LogoutController {
 private final TokenRevocationService tokens;
 @PostMapping("/api/auth/logout") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("isAuthenticated()")
 public void logout(@RequestHeader("Authorization") String header) { tokens.revoke(header.substring(7)); }
}
