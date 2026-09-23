package com.careerflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final JwtParser parser;
    private final long expirationSeconds;
    private final String issuer;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds:900}") long expirationSeconds,
                      @Value("${app.jwt.issuer:careerflow}") String issuer) {
        this(secret, expirationSeconds, issuer, Clock.systemUTC());
    }

    JwtService(String secret, long expirationSeconds, String issuer, Clock clock) {
        try {
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be a Base64-encoded random key of at least 32 bytes");
        }
        if (expirationSeconds < 60 || expirationSeconds > 86400) {
            throw new IllegalStateException("JWT expiration must be between 60 and 86400 seconds");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT issuer is required");
        }
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
        this.clock = clock;
        this.parser = Jwts.parser().verifyWith(key)
                .requireIssuer(issuer).require("token_use", "access")
                .clock(() -> Date.from(clock.instant())).build();
    }

    public String issue(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder().subject(userId.toString()).issuer(issuer)
                .claim("token_use", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Long userId(String token) {
        var signed = parser.parseSignedClaims(token);
        if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
            throw new JwtException("Unsupported signing algorithm");
        }
        Claims claims = signed.getPayload();
        if (claims.getExpiration() == null || claims.getIssuedAt() == null) {
            throw new JwtException("Missing token lifetime");
        }
        try {
            long id = Long.parseLong(claims.getSubject());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new JwtException("Invalid subject");
        }
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}
