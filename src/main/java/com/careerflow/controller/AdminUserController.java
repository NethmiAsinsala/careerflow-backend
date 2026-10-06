package com.careerflow.controller;

import com.careerflow.dto.request.AdminUserPageRequest;
import com.careerflow.dto.response.PageResponse;
import com.careerflow.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/users") @RequiredArgsConstructor
public class AdminUserController {
 private final AdminUserService users;
 public record StatusRequest(@NotNull Boolean active) { }
 @GetMapping
 public PageResponse<AdminUserService.UserSummary> list(@Valid @ModelAttribute @ParameterObject AdminUserPageRequest request) {
  return users.list(request.getPage(),request.getSize());
 }
 @PatchMapping("/{userId}/status")
 public AdminUserService.UserSummary status(@PathVariable Long userId,@Valid @RequestBody StatusRequest request) {
  return users.setActive(userId,request.active());
 }
}
