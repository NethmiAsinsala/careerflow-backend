package com.careerflow.controller;
import com.careerflow.entity.Certification;
import com.careerflow.service.CertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController @RequestMapping("/api/job-seekers") @RequiredArgsConstructor
public class CertificationController {
 private final CertificationService service;
 @GetMapping("/me/certifications") public List<Certification> own(){return service.own();}
 @GetMapping("/{jobSeekerId}/certifications") public List<Certification> read(@PathVariable Long jobSeekerId){return service.read(jobSeekerId);}
 @PostMapping("/me/certifications") @ResponseStatus(HttpStatus.CREATED) public Certification create(@Valid @RequestBody CertificationService.Request request){return service.create(request);}
 @PutMapping("/me/certifications/{id}") public Certification update(@PathVariable Long id,@Valid @RequestBody CertificationService.Request request){return service.update(id,request);}
 @DeleteMapping("/me/certifications/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){service.delete(id);}
}
