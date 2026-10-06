package com.careerflow.dto.request;

import jakarta.validation.constraints.*;
import com.careerflow.validation.WebUrl;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalProfileRequest {
    @NotBlank
    @Size(max = 100)
    private String firstName;
    @NotBlank
    @Size(max = 100)
    private String lastName;
    @Pattern(regexp = "^$|(?=.*[0-9])[+0-9() .-]{7,30}", message = "Phone must contain 7 to 30 phone-number characters")
    @Size(max = 30)
    private String phone;
    @Size(max = 200)
    private String headline;
    @Size(max = 3000)
    private String summary;
    @Size(max = 255)
    private String location;
    @WebUrl
    @Size(max = 500)
    private String linkedinUrl;
    @WebUrl
    @Size(max = 500)
    private String githubUrl;
    @WebUrl
    @Size(max = 500)
    private String portfolioUrl;
}
