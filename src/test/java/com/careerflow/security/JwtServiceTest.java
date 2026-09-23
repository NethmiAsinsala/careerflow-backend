package com.careerflow.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private final SecretKey key = Jwts.SIG.HS256.key().build();
    private final String secret = Base64.getEncoder().encodeToString(key.getEncoded());
    private final Instant now = Instant.parse("2026-10-05T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final JwtService jwt = new JwtService(secret, 900, "careerflow", clock);

    @Test void roundTrip() {
        assertThat(jwt.userId(jwt.issue(42L))).isEqualTo(42L);
        assertThat(jwt.expirationSeconds()).isEqualTo(900);
    }
    @Test void rejectsExpiredToken() {
        String token = new JwtService(secret, 900, "careerflow",
                Clock.fixed(now.minusSeconds(901), ZoneOffset.UTC)).issue(42L);
        assertThatThrownBy(() -> jwt.userId(token)).isInstanceOf(JwtException.class);
    }
    @Test void rejectsWrongSignature() {
        String other = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
        String token = new JwtService(other, 900, "careerflow", clock).issue(42L);
        assertThatThrownBy(() -> jwt.userId(token)).isInstanceOf(JwtException.class);
    }
    @Test void rejectsWrongIssuer() {
        String token = new JwtService(secret, 900, "another-app", clock).issue(42L);
        assertThatThrownBy(() -> jwt.userId(token)).isInstanceOf(JwtException.class);
    }
    @Test void rejectsUnsignedToken() {
        String token = Jwts.builder().subject("42").issuer("careerflow").compact();
        assertThatThrownBy(() -> jwt.userId(token)).isInstanceOf(JwtException.class);
    }
    @Test void rejectsMissingExpiration() {
        String token = Jwts.builder().subject("42").issuer("careerflow").claim("token_use", "access")
                .issuedAt(Date.from(now)).signWith(key, Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.userId(token)).isInstanceOf(JwtException.class);
    }
    @Test void rejectsWrongTokenPurpose() {
        String token = Jwts.builder().subject("42").issuer("careerflow").claim("token_use", "refresh")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(900)))
                .signWith(key, Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.userId(token)).isInstanceOf(JwtException.class);
    }
    @Test void rejectsInvalidSubject() {
        String token = Jwts.builder().subject("not-an-id").issuer("careerflow").claim("token_use", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(900)))
                .signWith(key, Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.userId(token)).isInstanceOf(JwtException.class);
    }
    @Test void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwt.userId("not.a.token")).isInstanceOf(JwtException.class);
    }
    @Test void rejectsMissingOrWeakSecret() {
        assertThatThrownBy(() -> new JwtService("", 900, "careerflow", clock))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(Base64.getEncoder().encodeToString(new byte[16]),
                900, "careerflow", clock)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsUnsafeExpiration() {
        assertThatThrownBy(() -> new JwtService(secret, 0, "careerflow", clock))
                .isInstanceOf(IllegalStateException.class);
    }
}
