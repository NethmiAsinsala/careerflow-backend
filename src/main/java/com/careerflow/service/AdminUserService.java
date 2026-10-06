package com.careerflow.service;

import com.careerflow.dto.response.PageResponse;
import com.careerflow.entity.User;
import com.careerflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {
 private final UserRepository users;
 public record UserSummary(Long id, String email, String role, boolean active, LocalDateTime createdAt) { }
 public PageResponse<UserSummary> list(int page, int size) {
  return PageResponse.from(users.findAll(PageRequest.of(page, size, Sort.by("id").descending())).map(this::summary));
 }
 @Transactional
 public UserSummary setActive(Long userId, boolean active) {
  User user = users.findByIdForAdminUpdate(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
  if ("ADMIN".equals(user.getRole().getName()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Admin account status cannot be changed through this endpoint");
  user.setActive(active);
  return summary(users.save(user));
 }
 private UserSummary summary(User user) {
  return new UserSummary(user.getId(),user.getEmail(),user.getRole().getName(),Boolean.TRUE.equals(user.getActive()),user.getCreatedAt());
 }
}
