package com.careerflow.service;
import com.careerflow.entity.Certification;
import com.careerflow.repository.*;
import com.careerflow.security.AuthenticatedUser;
import com.careerflow.validation.WebUrl;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("denyAll()")
public class CertificationService {
 private final CertificationRepository certifications;
 private final JobSeekerRepository seekers;

 public record Request(@NotBlank @Size(max=200) String name,@NotBlank @Size(max=200) String issuer,
                       @PastOrPresent LocalDate issuedOn,LocalDate expiresOn,@WebUrl @Size(max=500) String credentialUrl) { }

 @PreAuthorize("hasRole('JOB_SEEKER')") @Transactional(readOnly=true)
 public List<Certification> own(){
    return certifications.findByJobSeekerIdOrderByIdAsc(ownId());
 }

 @PreAuthorize("@resourceAccess.readsJobSeeker(#jobSeekerId)") @Transactional(readOnly=true)
 public List<Certification> read(Long jobSeekerId){
    return certifications.findByJobSeekerIdOrderByIdAsc(jobSeekerId);
 }

 @PreAuthorize("hasRole('JOB_SEEKER')")
 public Certification create(Request request){
    var cert=new Certification();
    cert.setJobSeekerId(ownId());
    fill(cert,request);
    return certifications.save(cert);
 }

 @PreAuthorize("hasRole('JOB_SEEKER')")
 public Certification update(Long id,Request request){
    var cert=owned(id);
    fill(cert,request);
    return certifications.save(cert);
 }

 @PreAuthorize("hasRole('JOB_SEEKER')")
 public void delete(Long id){
    certifications.delete(owned(id));
 }
 private Certification owned(Long id){
    var cert = certifications.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Certification not found"));
    if(!cert.getJobSeekerId().equals(ownId()))
     throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your certification");
    return cert;
 }
 private void fill(Certification cert,Request r){
    if(r.issuedOn()!=null && r.expiresOn()!=null && r.expiresOn().isBefore(r.issuedOn()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Expiry must not precede issue date");
    cert.setName(r.name().strip());cert.setIssuer(r.issuer().strip());cert.setIssuedOn(r.issuedOn());cert.setExpiresOn(r.expiresOn());
    cert.setCredentialUrl(r.credentialUrl()==null || r.credentialUrl().isBlank()?null:r.credentialUrl().strip());
 }
 private Long ownId(){
     var u=(AuthenticatedUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
     return seekers.findByUserId(u.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found")).getId();
 }
}
