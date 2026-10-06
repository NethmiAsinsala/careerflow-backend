package com.careerflow;

import com.careerflow.entity.*;
import com.careerflow.repository.*;
import com.careerflow.security.JwtService;
import com.careerflow.security.AuthenticatedUser;
import com.careerflow.service.JobService;
import com.careerflow.dto.request.JobRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "CAREERFLOW_DB_TESTS", matches = "true")
class ResumeIntegrationTest {
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        String secret = Base64.getEncoder().encodeToString(io.jsonwebtoken.Jwts.SIG.HS256.key().build().getEncoded());
        registry.add("app.jwt.secret", () -> secret);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired JobSeekerRepository seekers;
    @Autowired EmployerRepository employers;
    @Autowired CompanyRepository companies;
    @Autowired JobRepository jobs;
    @Autowired JobApplicationRepository applications;
    @Autowired SavedJobRepository savedJobs;
    @Autowired SkillRepository skills;
    @Autowired EducationRepository education;
    @Autowired ExperienceRepository experience;
    @Autowired ProjectRepository projects;
    @Autowired JobService jobService;

    User candidateUser, otherCandidateUser, employerUser, otherEmployerUser, admin;
    JobSeeker candidate, otherCandidate;
    Employer employer, otherEmployer;
    Job job, otherJob;
    Company company, otherCompany;
    JobApplication application, otherApplication;

    private User account(String role) {
        User user = new User();
        user.setEmail("access-test-" + UUID.randomUUID() + "@example.com");
        user.setPassword("unused-test-hash");
        user.setRole(roles.findByName(role).orElseThrow());
        return users.saveAndFlush(user);
    }
    private JobSeeker seeker(User user) {
        JobSeeker result = new JobSeeker();result.setUser(user);return seekers.saveAndFlush(result);
    }
    @BeforeEach void setup() {
        candidateUser = account("JOB_SEEKER"); otherCandidateUser = account("JOB_SEEKER");
        employerUser = account("EMPLOYER"); otherEmployerUser = account("EMPLOYER"); admin = account("ADMIN");
        candidate = seeker(candidateUser); otherCandidate = seeker(otherCandidateUser);
        employer = employers.saveAndFlush(Employer.builder().user(employerUser).build());
        otherEmployer = employers.saveAndFlush(Employer.builder().user(otherEmployerUser).build());
        company = companies.saveAndFlush(Company.builder().employer(employer).name("Owner company").build());
        otherCompany = companies.saveAndFlush(Company.builder().employer(otherEmployer).name("Other company").build());
        employer.setCompany(company);
        otherEmployer.setCompany(otherCompany);
        job = jobs.saveAndFlush(Job.builder().employer(employer).title("Owner job").status("OPEN").build());
        otherJob = jobs.saveAndFlush(Job.builder().employer(otherEmployer).title("Other job").status("OPEN").build());
        application = applications.saveAndFlush(JobApplication.builder().job(job).jobSeeker(candidate).status("APPLIED").build());
        otherApplication = applications.saveAndFlush(JobApplication.builder().job(otherJob).jobSeeker(otherCandidate).status("APPLIED").build());
    }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }
    private ResultActions call(User user, MockHttpServletRequestBuilder request) throws Exception {
        if (user != null) request.header("Authorization", "Bearer " + jwt.issue(user.getId()));
        return mvc.perform(request);
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Map<String, ?> body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }


    private byte[] pdf(String text) { return ("%PDF-1.7\n"+text+"\n%%EOF").getBytes(java.nio.charset.StandardCharsets.US_ASCII); }
    private MockHttpServletRequestBuilder upload(byte[] data) {
        return multipart("/api/job-seekers/me/resume").file(new org.springframework.mock.web.MockMultipartFile("file","../../resume.pdf","application/pdf",data));
    }
    @Test void uploadDownloadReplaceAndDelete() throws Exception {
        byte[] first=pdf("first"),second=pdf("replacement");
        call(candidateUser,upload(first)).andExpect(status().isOk()).andExpect(jsonPath("$.sizeBytes").value(first.length));
        call(candidateUser,get("/api/job-seekers/me/resume")).andExpect(status().isOk()).andExpect(content().bytes(first))
            .andExpect(header().string("Content-Disposition","attachment; filename=\"resume.pdf\""))
            .andExpect(header().string("Cache-Control","no-store"));
        call(candidateUser,upload(second)).andExpect(status().isOk());
        call(candidateUser,get("/api/job-seekers/me/resume")).andExpect(content().bytes(second));
        call(candidateUser,delete("/api/job-seekers/me/resume")).andExpect(status().isNoContent());
        call(candidateUser,get("/api/job-seekers/me/resume")).andExpect(status().isNotFound());
        call(candidateUser,delete("/api/job-seekers/me/resume")).andExpect(status().isNoContent());
    }
    @Test void restrictsApplicantDownloadsAndWrites() throws Exception {
        byte[] data=pdf("private");call(candidateUser,upload(data)).andExpect(status().isOk());
        String path="/api/job-seekers/"+candidate.getId()+"/resume";
        call(employerUser,get(path)).andExpect(status().isOk()).andExpect(content().bytes(data));
        call(admin,get(path)).andExpect(status().isOk());
        call(otherEmployerUser,get(path)).andExpect(status().isForbidden());
        call(otherCandidateUser,get(path)).andExpect(status().isForbidden());
        call(null,get(path)).andExpect(status().isUnauthorized());
        call(employerUser,upload(data)).andExpect(status().isForbidden());
        call(null,upload(data)).andExpect(status().isUnauthorized());
        call(employerUser,delete("/api/job-seekers/me/resume")).andExpect(status().isForbidden());
        call(otherCandidateUser,get("/api/job-seekers/me/resume")).andExpect(status().isNotFound());
    }
    @Test void rejectsEmptyNonPdfAndOversizeWithoutReplacing() throws Exception {
        byte[] valid=pdf("existing");call(candidateUser,upload(valid)).andExpect(status().isOk());
        call(candidateUser,upload(new byte[0])).andExpect(status().isBadRequest());
        call(candidateUser,upload("fake.pdf".getBytes())).andExpect(status().isUnsupportedMediaType());
        call(candidateUser,upload("%PDF-1.7 truncated".getBytes())).andExpect(status().isUnsupportedMediaType());
        call(candidateUser,upload(new byte[5*1024*1024+1])).andExpect(status().isPayloadTooLarge());
        call(candidateUser,get("/api/job-seekers/me/resume")).andExpect(content().bytes(valid));
    }
}
