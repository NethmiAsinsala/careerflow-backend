package com.careerflow.service;
import com.careerflow.entity.ContentReport;
import com.careerflow.repository.*;
import com.careerflow.dto.response.PageResponse;
import com.careerflow.security.AuthenticatedUser;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("denyAll()")
public class ContentReportService {
 private final ContentReportRepository reports;
 private final JobRepository jobs;
 private final CompanyRepository companies;
 private final UserRepository users;
 public record Request(@NotNull @Pattern(regexp="JOB|COMPANY|USER") String targetType,@NotNull @Positive Long targetId,@NotBlank @Size(max=2000) String reason) { }
 public record Resolution(@NotNull @Pattern(regexp="REVIEWED|DISMISSED") String status,@NotBlank @Size(max=2000) String resolution) { }
 @PreAuthorize("isAuthenticated()")
 public ContentReport create(Request request){
  boolean exists=switch(request.targetType()){case "JOB"->jobs.existsById(request.targetId());case "COMPANY"->companies.existsById(request.targetId());case "USER"->users.existsById(request.targetId());default->false;};
  if(!exists)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Reported target not found");
  var r=new ContentReport();r.setReporterId(currentId());r.setTargetType(request.targetType());r.setTargetId(request.targetId());r.setReason(request.reason().strip());
  r.setStatus("OPEN");r.setCreatedAt(LocalDateTime.now());r.setUpdatedAt(r.getCreatedAt());return reports.save(r);
 }
 @PreAuthorize("isAuthenticated()") @Transactional(readOnly=true)
 public PageResponse<ContentReport> mine(int page,int size){return PageResponse.from(reports.findByReporterId(currentId(),paging(page,size)));}
 @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true)
 public PageResponse<ContentReport> all(int page,int size){return PageResponse.from(reports.findAll(paging(page,size)));}
 @PreAuthorize("hasRole('ADMIN')")
 public ContentReport resolve(Long id,Resolution request){
  var r=reports.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Report not found"));
  if(!"OPEN".equals(r.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Report is already resolved");
  r.setStatus(request.status());r.setResolution(request.resolution().strip());r.setUpdatedAt(LocalDateTime.now());return reports.save(r);
 }
 private Pageable paging(int page,int size){return PageRequest.of(page,size,Sort.by("id").descending());}
 private Long currentId(){return ((AuthenticatedUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();}
}
