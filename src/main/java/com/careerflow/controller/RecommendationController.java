package com.careerflow.controller;
import com.careerflow.service.RecommendationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
@RestController @RequiredArgsConstructor
public class RecommendationController {
 private final RecommendationService service;
 @Getter @Setter public static class Request { @Min(1) @Max(50) private int limit=20; }
 @GetMapping("/api/job-seekers/me/recommendations")
 public RecommendationService.Recommendations recommend(@Valid @ModelAttribute @ParameterObject Request request){return service.recommend(request.getLimit());}
}
