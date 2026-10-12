package com.careerflow.controller;
import com.careerflow.service.ContentReportService;
import com.careerflow.entity.ContentReport;
import com.careerflow.dto.request.AdminUserPageRequest;
import com.careerflow.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springdoc.core.annotations.ParameterObject;
@RestController @RequiredArgsConstructor
public class ContentReportController {
 private final ContentReportService service;
 @PostMapping("/api/reports") @ResponseStatus(HttpStatus.CREATED)
 public ContentReport create(@Valid @RequestBody ContentReportService.Request request){return service.create(request);}
 @GetMapping("/api/reports/me")
 public PageResponse<ContentReport> mine(@Valid @ModelAttribute @ParameterObject AdminUserPageRequest p){return service.mine(p.getPage(),p.getSize());}
 @GetMapping("/api/admin/reports")
 public PageResponse<ContentReport> all(@Valid @ModelAttribute @ParameterObject AdminUserPageRequest p){return service.all(p.getPage(),p.getSize());}
 @PatchMapping("/api/admin/reports/{id}")
 public ContentReport resolve(@PathVariable Long id,@Valid @RequestBody ContentReportService.Resolution request){return service.resolve(id,request);}
}
