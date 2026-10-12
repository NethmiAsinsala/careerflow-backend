package com.careerflow.security;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.sql.Timestamp;
import java.time.Instant;
@Service @RequiredArgsConstructor
public class TokenRevocationService {
 private final JdbcTemplate jdbc;
 private final JwtService jwt;
 public boolean isRevoked(String token) {
  return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from revoked_tokens where token_hash = ?)",Boolean.class,hash(token)));
 }
 @Transactional
 public void revoke(String token) {
  var expiry=jwt.expiresAt(token);
  jdbc.update("delete from revoked_tokens where expires_at < ?",Timestamp.from(Instant.now()));
  jdbc.update("insert ignore into revoked_tokens(token_hash, expires_at) values (?, ?)",hash(token),Timestamp.from(expiry));
 }
 private String hash(String token) {
  try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException exception){throw new IllegalStateException(exception);}
 }
}
