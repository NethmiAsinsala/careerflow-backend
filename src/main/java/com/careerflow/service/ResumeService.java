package com.careerflow.service;
import com.careerflow.entity.Resume;
import com.careerflow.repository.*;
import com.careerflow.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Transactional @PreAuthorize("denyAll()")
public class ResumeService {
 public static final int MAX_BYTES=5*1024*1024;
 private final ResumeRepository resumes;
 private final JobSeekerRepository seekers;
 public record Metadata(Long jobSeekerId,String filename,int sizeBytes,LocalDateTime updatedAt) { }
 @PreAuthorize("hasRole('JOB_SEEKER')")
 public Metadata upload(MultipartFile file) {
  if(file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Resume must not be empty");
  if(file.getSize()>MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Resume must be at most 5 MB");
  byte[] content;
  try(var stream=file.getInputStream()) { content=stream.readNBytes(MAX_BYTES+1); }
  catch(IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Could not read resume"); }
  if(content.length>MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Resume must be at most 5 MB");
  String start=new String(content,0,Math.min(content.length,8),StandardCharsets.US_ASCII);
  String tail=new String(content,Math.max(0,content.length-1024),Math.min(content.length,1024),StandardCharsets.US_ASCII);
  if(!start.startsWith("%PDF-") || !tail.contains("%%EOF"))
   throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Upload a PDF resume");
  Long id=ownId();
  // Lock the owner even on the first upload so concurrent replacements serialize.
  seekers.findByIdForResumeUpdate(id).orElseThrow();
  Resume resume=resumes.findById(id).orElseGet(Resume::new);
  resume.setJobSeekerId(id);resume.setContent(content);resume.setUpdatedAt(LocalDateTime.now());
  resumes.save(resume);
  return new Metadata(id,"resume.pdf",content.length,resume.getUpdatedAt());
 }
 @Transactional(readOnly=true) @PreAuthorize("hasRole('JOB_SEEKER')")
 public byte[] downloadOwn() { return content(ownId()); }
 @Transactional(readOnly=true) @PreAuthorize("@resourceAccess.readsJobSeeker(#jobSeekerId)")
 public byte[] download(Long jobSeekerId) { return content(jobSeekerId); }
 @PreAuthorize("hasRole('JOB_SEEKER')")
 public void deleteOwn() {
  Long id=ownId();seekers.findByIdForResumeUpdate(id).orElseThrow();
  resumes.findById(id).ifPresent(resumes::delete);
 }
 private byte[] content(Long id) {
  return resumes.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No resume uploaded")).getContent();
 }
 private Long ownId() {
  var principal=(AuthenticatedUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  return seekers.findByUserId(principal.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found")).getId();
 }
}
