package com.careerflow.controller;
import com.careerflow.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
@RestController @RequestMapping("/api/job-seekers") @RequiredArgsConstructor
public class ResumeController {
 private final ResumeService resumes;
 @PostMapping(value="/me/resume",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResumeService.Metadata upload(@RequestPart("file") MultipartFile file) { return resumes.upload(file); }
 @GetMapping("/me/resume")
 public ResponseEntity<byte[]> downloadOwn() { return attachment(resumes.downloadOwn()); }
 @GetMapping("/{jobSeekerId}/resume")
 public ResponseEntity<byte[]> download(@PathVariable Long jobSeekerId) { return attachment(resumes.download(jobSeekerId)); }
 @DeleteMapping("/me/resume") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete() { resumes.deleteOwn(); }
 private ResponseEntity<byte[]> attachment(byte[] content) {
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(content.length)
   .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"resume.pdf\"")
   .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").body(content);
 }
}
