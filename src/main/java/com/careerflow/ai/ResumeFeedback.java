package com.careerflow.ai;
import jakarta.validation.constraints.*;
import java.util.List;
public record ResumeFeedback(
    @NotBlank @Size(max=2000) String summary,
    @NotNull @Size(max=40) List<@NotBlank @Size(max=100) String> skills,
    @NotNull @Size(max=10) List<@NotBlank @Size(max=500) String> strengths,
    @NotNull @Size(max=10) List<@NotBlank @Size(max=500) String> improvements) { }
