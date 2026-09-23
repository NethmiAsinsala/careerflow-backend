package com.careerflow.security;

import com.careerflow.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthService authService;
    private final ApiSecurityErrorHandler errorHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // An expired token must not prevent obtaining a new token with credentials.
        return "POST".equals(request.getMethod()) &&
                ("/api/auth/login".equals(request.getServletPath()) ||
                 "/api/auth/register".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() <= 7) {
                    throw new BadCredentialsException("Invalid bearer header");
                }
                Long userId = jwtService.userId(header.substring(7));
                AuthenticatedUser user = authService.authenticatedUser(userId);
                var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
                SecurityContextHolder.clearContext();
                errorHandler.commence(request, response, new BadCredentialsException("Invalid access token"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
